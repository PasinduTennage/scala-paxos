package paxos.shared

import upickle.default._

case class Message(senderId: Int, messages: List[String])

object Message {
  implicit val rw: ReadWriter[Message] = macroRW
}
