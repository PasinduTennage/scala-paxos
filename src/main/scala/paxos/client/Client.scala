package paxos.client

import java.io.{BufferedReader, InputStreamReader, PrintWriter}
import java.net.Socket
import paxos.shared.Message
import upickle.default._

class Client(port: Int) {

  var readers: List[BufferedReader] = Nil // Default value; will be set later

  def setReaders(rs: List[BufferedReader]): Unit = {
    this.readers = rs
  }


  def start(): Unit = {
    val socket = new Socket("localhost", port)
    val in = new BufferedReader(new InputStreamReader(socket.getInputStream))
    val out = new PrintWriter(socket.getOutputStream, true)

    val senderId = 1
    var counter = 0

    while (true) {
      counter += 1
      val msg = Message(senderId, List(s"hello $counter", s"msg $counter"))
      val json = write(msg)
      out.println(json)
      val response = in.readLine()
      println(s"Server responded: $response")
      Thread.sleep(1000)
    }
  }
}
