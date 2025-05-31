package paxos.client

import java.io.{BufferedReader, InputStreamReader, PrintWriter}
import java.net.Socket

object Main{
  def main(args: Array[String]):Unit = {
    new Client().start()
  }
}