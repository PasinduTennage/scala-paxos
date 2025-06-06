package paxos.shared

import upickle.default._
import ujson.Str

sealed trait Message

case class HeartBeat(
  senderId: Int) extends Message

case class Id(
  senderId: Int) extends Message

case class ClientBatch(
  senderId: Int, commands: List[String]) extends Message

case class ReplicaBatch(
  proposalId: String, commands: List[ClientBatch]) extends Message  

case class Prepare(
  senderId: Int, 
  instance: Int, 
  prepareBallot: Int)extends Message

case class Promise(
    senderId: Int,
    instance: Int,
    promiseBallot: Int,
    lastAcceptedBallot: Int,
    lastAcceptedValue: List[ClientBatch]
) extends Message

case class Propose(
    instance: Int,
    proposeBallot: Int,
    proposeId: String,
    proposeValue: List[ClientBatch]) extends Message
    

case class Accept(
  instance: Int, 
  acceptBallot: Int) extends Message

case class Decide(
  instance: Int, 
  decideId: String) extends Message

case class FetchRequest(
  instance: Int, 
  decideId: String) extends Message

case class FetchResponse(
    instance: Int,
    decideId: String,
    value: List[ClientBatch]) extends Message

object Message {
  implicit val clientBatchRW: ReadWriter[ClientBatch] = macroRW
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
