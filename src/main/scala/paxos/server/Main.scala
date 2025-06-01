package paxos.server

import paxos.config.JsonLoader

object Main {
  def main(args: Array[String]): Unit = {
    if (args.length < 2) {
      println("Usage: ./server <name> <config-path>")
      System.exit(-1)
    }

    val id = args(0).toInt
    val configPath = args(1)

    val config = JsonLoader.load(configPath)
    val maybeSelf = config.peers.find(_.name == id)

    if (maybeSelf.isEmpty) {
      println(s"No peer named $id found in config")
      System.exit(-1)
    }

    val port = maybeSelf.get.address.split(":")(1).toInt
    val server = new Server(port)
    server.start()
  }
}
