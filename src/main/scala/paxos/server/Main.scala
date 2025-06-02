package paxos.server

import paxos.config.{JsonLoader, Peer}
import scopt.OParser

case class CmdArgs(name: Int = 1, configPath: String = "src/main/resources/config.json")

object Main {
  def main(args: Array[String]): Unit = {
    val builder = OParser.builder[CmdArgs]
    val parser = {
      import builder._
      OParser.sequence(
        programName("server"),
        head("Paxos Server", "1.0"),
        opt[Int]("name")
          .action((x, c) => c.copy(name = x))
          .text("server id name as integer"),
        opt[String]("config")
          .action((x, c) => c.copy(configPath = x))
          .text("path to config.json")
      )
    }

    OParser.parse(parser, args, CmdArgs()) match {
      case Some(cmdArgs) =>
        val config = JsonLoader.load(cmdArgs.configPath)
        val maybeSelf = config.peers.find(_.name == cmdArgs.name)

        if (maybeSelf.isEmpty) {
          println(s"No peer named ${cmdArgs.name} found in config")
          System.exit(-1)
        }

        val port = maybeSelf.get.address.split(":")(1).toInt
        new Server(port).start()

      case None =>
        System.exit(1)
    }
  }
}
