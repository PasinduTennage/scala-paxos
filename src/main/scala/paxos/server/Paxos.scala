package paxos.server

import paxos.shared.ReplicaBatch

import paxos.shared._

import upickle.default._
import scala.collection.immutable.Map
import scala.collection.mutable.ListBuffer

import java.time.LocalDateTime

// slot is a single entry in the replicated log
class Slot {
  var index: Int = -1

  var decided: Boolean = false
  var decided_id: String = null

  var prepared_ballot: Int = -1
  var promised_ballot: Int = -1
  var highest_seen_accepted_ballot: Int = -1
  var highest_seen_accepted_value: ReplicaBatch = null
  var num_prepare_reponses: Int = 0

  var proposed_ballot: Int = -1
  var proposed_value_id: String = null
  var num_accept_reponses: Int = 0

  var accepted_ballot: Int = -1
  var accepted_value_id: String = null
}

class Paxos(val n: Int, val server: Server) {

  var is_proposing: Boolean = false // whether this replica is proposing a value

  var last_proposed_time: LocalDateTime =
    LocalDateTime.now() // last time a value was proposed

  val quorum: Int = n / 2 + 1

  var id_replicaBatch_map: Map[String, ReplicaBatch] = Map.empty

  // replicated log
  var slots = ListBuffer.empty[Slot]

  // last decided index
  var last_decided_index: Int = -1

  val last_id = 1 // last id used for creating ReplicaBatch

  var incomingClientBatches =
    ListBuffer.empty[ClientBatch] // client batches to be proposed later

  def create_instance_if_not_exists(index: Int): Unit = {
    if (index >= this.slots.length) {
      for (i <- this.slots.length to index) {
        this.slots += new Slot()
      }
    }
  }

  // send a Prepare message to all replicas

  def send_prepare(): Unit = {

    this.last_proposed_time = LocalDateTime.now()
    val prepare_instance = this.last_decided_index + 1

    this.create_instance_if_not_exists(prepare_instance)

    this.slots(prepare_instance).highest_seen_accepted_ballot = -1
    this.slots(prepare_instance).highest_seen_accepted_value = null
    this.slots(prepare_instance).num_prepare_reponses = 0

    this.slots(prepare_instance).proposed_ballot = -1
    this.slots(prepare_instance).proposed_value_id = null
    this.slots(prepare_instance).num_accept_reponses = 0

    var prepare_ballot = this
      .slots(prepare_instance)
      .prepared_ballot + this
      .slots(prepare_instance)
      .promised_ballot + 3 //  because of initial -2

    this.slots(prepare_instance).prepared_ballot = prepare_ballot

    // broadcast a prepare message to all replicas

    val msg = Prepare(
      senderId = this.server.name,
      instance = prepare_instance,
      prepareBallot = prepare_ballot
    )
    val json = write[Message](msg)

    this.server.config.peers.foreach { peer =>
      {
        this.server.replicaWriters(peer.name).println(json)
      }
    }

  }

  // handle prepare message

  def handle_prepare(m: Prepare): Unit = {
    this.create_instance_if_not_exists(m.instance)

    if (this.slots(m.instance).decided) {

      // send a FetchReponse, only if the conrresponding ReplicaBatch exists
      if (
        (this.id_replicaBatch_map.contains(this.slots(m.instance).decided_id))
      ) {
        // send a FetchResponse

        val msg = FetchResponse(
          instance = m.instance,
          decidedValue = this.id_replicaBatch_map
            .getOrElse(this.slots(m.instance).decided_id, null)
        )

        val json = write[Message](msg)

        this.server
          .replicaWriters(m.senderId)
          .println(json)
      }

    } else {
      if (m.prepareBallot > this.slots(m.instance).promised_ballot) {
        // update the slot with the new promised ballot
        this.slots(m.instance).promised_ballot = m.prepareBallot

        // send a Promise message back to the sender
        val msg = Promise(
          senderId = this.server.name,
          instance = m.instance,
          promiseBallot = this.slots(m.instance).promised_ballot,
          lastAcceptedBallot = this.slots(m.instance).accepted_ballot,
          lastAcceptedValue = this.id_replicaBatch_map
            .getOrElse(this.slots(m.instance).accepted_value_id, null)
        )

        val json = write[Message](msg)

        this.server.replicaWriters(m.senderId).println(json)

      }
    }
  }

  def handle_promise(m: Promise): Unit = {

    // if m.instance is not in slots, thorow run time  exception

    if (m.instance < 0 || m.instance >= this.slots.length) {
      throw new RuntimeException(
        s"Instance ${m.instance} is out of bounds for slots length ${this.slots.length}"
      )
    }

    if (this.slots(m.instance).decided) {
      return
    }

    if (m.promiseBallot == this.slots(m.instance).prepared_ballot) {

      this.slots(m.instance).num_prepare_reponses += 1

      if (
        m.lastAcceptedBallot > this
          .slots(m.instance)
          .highest_seen_accepted_ballot
      ) {
        this.slots(m.instance).highest_seen_accepted_ballot =
          m.lastAcceptedBallot
        this.slots(m.instance).highest_seen_accepted_value = m.lastAcceptedValue
      }

      if (this.slots(m.instance).num_prepare_reponses == this.quorum) {

        val propose_ballot = this.slots(m.instance).prepared_ballot
        this.slots(m.instance).proposed_ballot = propose_ballot

        var propose_value = this.slots(m.instance).highest_seen_accepted_value
        if (this.slots(m.instance).highest_seen_accepted_ballot == -1) {

          // create a new ReplicaBatch with the next id
          // commands contains upto replicaBatchSize numner of ClientBatches, remove the selected batches from incomingClientBatches
          propose_value = ReplicaBatch(
            Id = s"${this.server.name}:${this.last_id}",
            commands = this.incomingClientBatches
              .take(this.server.replicaBathSize)
              .toList
          )
          this.incomingClientBatches =
            this.incomingClientBatches.drop(this.server.replicaBathSize)
        }

        val msg = Propose(
          instance = m.instance,
          proposeBallot = propose_ballot,
          proposeValue = propose_value,
          senderId = this.server.name
        )

        val json = write[Message](msg)

        this.server.config.peers.foreach { peer =>
          {
            this.server.replicaWriters(peer.name).println(json)
          }
        }

      }
    }
  }

  // handle propose message

  def handle_propose(m: Propose): Unit = {

    this.create_instance_if_not_exists(m.instance)

    if (this.slots(m.instance).decided) {
      return
    }

    if (m.proposeBallot >= this.slots(m.instance).promised_ballot) {
      this.slots(m.instance).accepted_ballot = m.proposeBallot
      // put m.proposeValue in id_replicaBatch_map
      this.id_replicaBatch_map += (m.proposeValue.Id -> m.proposeValue)
      this.slots(m.instance).accepted_value_id = m.proposeValue.Id

      // send an Accept message

      val msg = Accept(
        instance = m.instance,
        acceptBallot = m.proposeBallot
      )

      val json = write[Message](msg)

      this.server.replicaWriters(m.senderId).println(json)

    }

  }

  // handle accept message

  def handle_accept(m: Accept): Unit = {
    if (m.instance < 0 || m.instance >= this.slots.length) {
      throw new RuntimeException(
        s"Instance ${m.instance} is out of bounds for slots length ${this.slots.length}"
      )
    }

    if (this.slots(m.instance).decided) {
      return
    }

    if (m.acceptBallot == this.slots(m.instance).proposed_ballot) {
      this.slots(m.instance).num_accept_reponses += 1

      if (
        this.slots(m.instance).num_accept_reponses == this.quorum && !this
          .slots(m.instance)
          .decided
      ) {
        // decide the value
        this.slots(m.instance).decided = true
        this.slots(m.instance).decided_id =
          this.slots(m.instance).accepted_value_id
        this.is_proposing = false
        this.update_smr()

        // send a Decide message to all replicas
        val msg = Decide(
          instance = m.instance,
          Id = this.slots(m.instance).decided_id
        )

        val json = write[Message](msg)

        this.server.config.peers.foreach { peer =>
          {
            this.server.replicaWriters(peer.name).println(json)
          }
        }
      }
    }
  }

  // handle decide message

  def handle_decide(m: Decide): Unit = {
    this.create_instance_if_not_exists(m.instance)

    if (this.slots(m.instance).decided) {
      return
    }

    this.slots(m.instance).decided = true
    this.slots(m.instance).decided_id = m.Id

    this.update_smr()

    // if the decided value is not in id_replicaBatch_map, then send a FetchRequest

    if (!this.id_replicaBatch_map.contains(m.Id)) {
      val msg = FetchRequest(sender = this.server.name, instance = m.instance)

      val json = write[Message](msg)

      // send fetch reqiest to a random replicr

      var replicaIndex =
        scala.util.Random.nextInt(this.server.config.peers.length) + 1

      while (replicaIndex == this.server.name) {
        replicaIndex =
          scala.util.Random.nextInt(this.server.config.peers.length) + 1
      }

      this.server.replicaWriters(replicaIndex).println(json)
    }
  }

  // handle fetch request message

  def handle_fetch_request(m: FetchRequest): Unit = {

    if (m.instance >= this.slots.length) {
      return
    }

    val decidedValue = this.id_replicaBatch_map
      .getOrElse(this.slots(m.instance).decided_id, null)

    if (decidedValue != null) {
      // send a FetchResponse
      val msg = FetchResponse(
        instance = m.instance,
        decidedValue = decidedValue
      )

      val json = write[Message](msg)

      this.server.replicaWriters(m.sender).println(json)
    }

  }

  // handle fetch response message

  def handle_fetch_response(m: FetchResponse): Unit = {
    if (m.instance >= this.slots.length) {
      throw new RuntimeException(
        s"Instance ${m.instance} is out of bounds for slots length ${this.slots.length}"
      )
    }

    // put the decided value in id_replicaBatch_map
    this.id_replicaBatch_map += (m.decidedValue.Id -> m.decidedValue)

    this.update_smr()
  }

  // update smr

  def update_smr(): Unit = {
    // update the state machine

    var i = this.last_decided_index + 1

    while (i < this.slots.length && this.slots(i).decided) {

      if (!this.id_replicaBatch_map.contains(this.slots(i).decided_id)) {
        // send a FetchRequest to a random replica
        val msg = FetchRequest(sender = this.server.name, instance = i)
        var random_sender =
          scala.util.Random.nextInt(this.server.config.peers.length) + 1
        while (random_sender == this.server.name) {
          random_sender =
            scala.util.Random.nextInt(this.server.config.peers.length) + 1
        }
        val json = write[Message](msg)
        this.server.replicaWriters(random_sender).println(json)
        return
      } else {
        this.last_decided_index = i
      }

    }

  }

}
