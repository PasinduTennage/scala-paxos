package paxos.paxos

import paxos.shared.ReplicaBatch

// slot is a single entry in the replicated log
class Slot {
  var index: Int = -1

  var decided: Boolean = false
  var decided_id: String = null

  var prepared_ballot: Int = -1
  var highest_seen_accepted_ballot: Int = -1
  var highest_seen_accepted_value: String = null
  var num_prepare_reponses: Int = 0

  var proposed_ballot: Int = -1
  var proposed_value_id: String = null
  var num_accept_reponses: Int = 0

  var accepted_ballot: Int = -1
  var accepted_value_id: String = null
}

class Paxos {

  // number of replicas in the system
  var numReplicas: Int = config.peers.length

  // current view number
  var view: Int = 0

  // last decided index
  var lastDecidedIndex: Int = -1

  // replicated log of

}
