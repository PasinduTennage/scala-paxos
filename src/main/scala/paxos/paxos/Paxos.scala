package paxos.paxos

import paxos.shared.ReplicaBatch

import scala.collection.immutable.Map
import scala.collection.mutable.ListBuffer
import paxos.shared._
import paxos.server.Server
import upickle.default._

// slot is a single entry in the replicated log
class Slot {
  var index: Int = -1

  var decided: Boolean = false
  var decided_id: String = null

  var prepared_ballot: Int = -1
  var promised_ballot: Int = -1
  var highest_seen_accepted_ballot: Int = -1
  var highest_seen_accepted_value: String = null
  var num_prepare_reponses: Int = 0

  var proposed_ballot: Int = -1
  var proposed_value_id: String = null
  var num_accept_reponses: Int = 0

  var accepted_ballot: Int = -1
  var accepted_value_id: String = null
}

class Paxos(val n: Int, val server: Server) {

  var id_replicaBatch: Map[String, ReplicaBatch] = Map.empty

  // replicated log
  var slots = ListBuffer.empty[Slot]

  // current view number
  var view: Int = 0

  // last decided index
  var last_decided_index: Int = -1

  var current_leader = 1 // initial leader is replica 1

  def get_current_leader(): Int = {
    this.current_leader
  }

  def create_instance_if_not_exists(index: Int): Unit = {
    if (index >= this.slots.length) {
      for (i <- this.slots.length to index) {
        this.slots += new Slot()
      }
    }
  }

  def send_prepare(): Unit = {
    val prepare_instance = this.last_decided_index + 1
    this.create_instance_if_not_exists(prepare_instance)
    var prepare_ballot = this.slots(prepare_instance).prepared_ballot + 1
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

  def handle_prepare(m: Prepare): Unit = {
    this.create_instance_if_not_exists(m.instance)

    if (this.slots(m.instance).decided) {
      // send a FetchResponse
      this.server
        .replicaWriters(m.senderId)
        .println(
          write[Message](
            FetchResponse(
              instance = m.instance,
              decidedValue =
                this.id_replicaBatch(this.slots(m.instance).decided_id)
            )
          )
        )
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
          lastAcceptedValue = this.id_replicaBatch.getOrElse(
            this.slots(m.instance).accepted_value_id,
            ReplicaBatch("", List())
          )
        )
        this.server.replicaWriters(m.senderId).println(write[Message](msg))
      }
    }

  }
}
