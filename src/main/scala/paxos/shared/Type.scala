package paxos.shared

import upickle.default._

case class Prepare(
                    senderId: Int,
                    instance: Int,
                    prepareBallot: Int)

object Prepare {
  implicit val rw: ReadWriter[Prepare] = macroRW
}

case class Promise(
                    senderId: Int,
                    instance: Int,
                    promiseBallot: Int,
                    lastAcceptedBallot: Int,
                    lastAcceptedValue: List[String])

object Promise {
  implicit val rw: ReadWriter[Promise] = macroRW
}

case class Propose(
                    instance: Int,
                    proposeBallot: Int,
                    proposeValue: List[String])

object Propose {
  implicit val rw: ReadWriter[Propose] = macroRW
}

case class Accept(
                   instance: Int,
                   acceptBallot: Int)

object Accept {
  implicit val rw: ReadWriter[Accept] = macroRW
}

case class Decide(
                   instance: Int,
                   decideBallot: Int)

object Decide {
  implicit val rw: ReadWriter[Decide] = macroRW
}

case class FetchRequest(
                         instance: Int,
                         decideBallot: Int)

object FetchRequest {
  implicit val rw: ReadWriter[FetchRequest] = macroRW
}

case class FetchResponse(
                          instance: Int,
                          decideBallot: Int,
                          value: List[String])

object FetchResponse {
  implicit val rw: ReadWriter[FetchResponse] = macroRW
}