package paxos.client

import java.io.{BufferedReader, InputStreamReader, PrintWriter}
import java.net.Socket

class Client {
  def start(): Unit = {
    val socket = new Socket("localhost", 9999)
    val in = new BufferedReader(new InputStreamReader(socket.getInputStream))
    val out = new PrintWriter(socket.getOutputStream, true)
    var counter = 1
    while (true) {
      out.println(s"${counter}")
      counter += 1
      println(in.readLine())
    }
  }
}
