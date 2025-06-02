package paxos.shared

import upickle.default._

sealed trait Message

case class Id(senderId: Int) extends Message

case class ClientBatch(
                        senderId: Int,
                        commands: List[String]) extends Message

case class Prepare(
                    senderId: Int,
                    instance: Int,
                    prepareBallot: Int) extends Message

case class Promise(
                    senderId: Int,
                    instance: Int,
                    promiseBallot: Int,
                    lastAcceptedBallot: Int,
                    lastAcceptedValue: List[String]) extends Message

case class Propose(
                    instance: Int,
                    proposeBallot: Int,
                    proposeValue: List[ClientBatch]) extends Message

case class Accept(
                   instance: Int,
                   acceptBallot: Int) extends Message


case class Decide(
                   instance: Int,
                   decideBallot: Int) extends Message


case class FetchRequest(
                         instance: Int,
                         decideBallot: Int) extends Message


case class FetchResponse(
                          instance: Int,
                          decideBallot: Int,
                          value: List[ClientBatch]) extends Message

object Message {
  implicit val rw: ReadWriter[Message] = ReadWriter.merge(
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