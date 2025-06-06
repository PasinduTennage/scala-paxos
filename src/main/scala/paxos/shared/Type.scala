package paxos.shared

import upickle.default._
import ujson.Str

sealed trait Message

case class HeartBeat(senderId: Int) extends Message

case class Id(senderId: Int) extends Message

case class ClientBatch(senderId: Int, commands: List[String]) extends Message

case class ReplicaBatch(Id: String, commands: List[ClientBatch]) extends Message

case class Prepare(senderId: Int, instance: Int, prepareBallot: Int)
    extends Message

case class Promise(
    senderId: Int,
    instance: Int,
    promiseBallot: Int,
    lastAcceptedBallot: Int,
    lastAcceptedValue: ReplicaBatch
) extends Message

case class Propose(
    instance: Int,
    proposeBallot: Int,
    proposeValue: ReplicaBatch
) extends Message

case class Accept(instance: Int, acceptBallot: Int) extends Message

case class Decide(instance: Int, Id: String) extends Message

case class FetchRequest(instance: Int) extends Message

case class FetchResponse(
    instance: Int,
    decidedValue: ReplicaBatch,
) extends Message

object Message {
  implicit val clientBatchRW: ReadWriter[ClientBatch] = macroRW
  implicit val replicaBatchRW: ReadWriter[ReplicaBatch] = macroRW
  implicit val rw: ReadWriter[Message] = ReadWriter.merge(
    macroRW[HeartBeat],
    macroRW[Id],
    macroRW[ClientBatch],
    macroRW[Prepare],
    macroRW[Promise],
    macroRW[Propose],
    macroRW[Accept],
    macroRW[Decide],
    macroRW[FetchRequest],
    macroRW[FetchResponse]
  )
}
