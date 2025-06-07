package paxos.client


import paxos.config._
import scopt.OParser
import paxos.config.JsonLoader



case class ClientCmdArgs(
    name: Int = 1,
    configPath: String = "config/config.json",
)

object Main {

  def main(args: Array[String]): Unit = {
    
    val builder = OParser.builder[ClientCmdArgs]

    val parser = {
      import builder._
      OParser.sequence(
        programName("client"),
        head("Client", "1.0"),
        opt[Int]("name")
          .action((x, c) => c.copy(name = x))
          .text("server name name as integer"),
        opt[String]("configPath")
          .action((x, c) => c.copy(configPath = x))
          .text("path to config.json")
      )
    }

    OParser.parse(parser, args, ClientCmdArgs()) match {
      case Some(clientCmdArgs) =>
        val config = JsonLoader.load(clientCmdArgs.configPath)
        val client = new Client(clientCmdArgs.name, config)
        client.start()       

      case None =>
        println("failed passing args")
        System.exit(1)
    }
  }


  
}
