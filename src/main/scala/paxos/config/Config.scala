package paxos.config

import java.io.File
import scala.io.Source
import upickle.default._

case class Peer(val name: Int, val ip: String, val port: Int)

object Peer {
  implicit val rw: ReadWriter[Peer] = macroRW
}

case class NetworkConfig(val peers: List[Peer])

object NetworkConfig {
  implicit val rw: ReadWriter[NetworkConfig] = macroRW
}

object JsonLoader {
  def load(path: String): NetworkConfig = {
    val source = Source.fromFile(new File(path))

    try {
      read[NetworkConfig](source.mkString)
    } finally {
      source.close()
    }

  }
}
