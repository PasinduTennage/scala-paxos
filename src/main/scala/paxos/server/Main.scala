package paxos.server

import paxos.config.{JsonLoader, Peer}
import scopt.OParser

case class CmdArgs(
                    name: Int = 1,
                    configPath: String = "config/config.json",
                    replicaBathSize: Int = 100,
                    replicaBatchTime: Int = 100, // ms
                    viewTimeOut: Int = 10000, // ms
                    logPath: String = "logs/",
                    debugLevel: Int = 0,
                    pipeLineLength: Int = 1 /*//todo pipelining implementation */)

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

        opt[String]("configPath")
          .action((x, c) => c.copy(configPath = x))
          .text("path to config.json"),

        opt[Int]("replicaBathSize")
          .action((x, c) => c.copy(replicaBathSize = x))
          .text("replicaBathSize as integer"),

        opt[Int]("replicaBatchTime")
          .action((x, c) => c.copy(replicaBatchTime = x))
          .text("replicaBatchTime as integer"),

        opt[Int]("viewTimeOut")
          .action((x, c) => c.copy(viewTimeOut = x))
          .text("viewTimeOut as integer"),

        opt[String]("logPath")
          .action((x, c) => c.copy(logPath = x))
          .text("logPath as string"),

        opt[Int]("debugLevel")
          .action((x, c) => c.copy(debugLevel = x))
          .text("debugLevel as integer"),

        opt[Int]("pipeLineLength")
          .action((x, c) => c.copy(pipeLineLength = x))
          .text("pipeLineLength as integer"),
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

        val port = maybeSelf.get.port

        val s: Server = new Server(port, cmdArgs.name, config, cmdArgs.replicaBathSize, cmdArgs.replicaBatchTime, cmdArgs.viewTimeOut, cmdArgs.logPath, cmdArgs.debugLevel, cmdArgs.pipeLineLength)
        s.initServer()

        while (true) {
          Thread.sleep(100)
        }

      case None =>
        println("failed passing args")
        System.exit(1)
    }
  }
}
