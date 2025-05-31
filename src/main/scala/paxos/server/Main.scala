package paxos.server

import scala.math.Integral.Implicits.infixIntegralOps

object Main {
  def main(args: Array[String]): Unit = {
    if (args.length < 1) {
      System.exit(-1)
    }

    val port = args(0).toInt
    val server = new Server(port)
    server.start()
  }
}
