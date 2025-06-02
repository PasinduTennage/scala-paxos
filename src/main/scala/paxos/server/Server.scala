package paxos.server

import paxos.config.NetworkConfig

import java.io.{BufferedReader, InputStreamReader, PrintWriter}
import java.net.{ServerSocket, Socket}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}
import paxos.shared.Message
import upickle.default._

class Server(port: Int, name: Int, config: NetworkConfig, replicaBathSize: Int, replicaBatchTime: Int, viewTimeOut: Int, logPath: String, debugLevel: Int, pipeLineLength: Int) {
  implicit val ec: ExecutionContext = ExecutionContext.global

  def start(): Unit = {
    val server = new ServerSocket(this.port)
    println(s"Server started on port $port")
    while (true) {
      val socket = server.accept()
      Future {
        handle(socket)
      }.onComplete {
        case Failure(e) => println("Failed: " + e.getMessage)
        case Success(_) => println("Success")
      }
    }
  }

  def handle(socket: Socket): Unit = {
    val in = new BufferedReader(new InputStreamReader(socket.getInputStream))
    val out = new PrintWriter(socket.getOutputStream, true)

    try {
      var line: String = in.readLine()
      while (line != null) {
        try {
          val msg = read[Message](line)
          println(s"Received from client ${msg.senderId}: ${msg.messages.mkString(", ")}")
          out.println("ack")
        } catch {
          case e: Exception =>
            println(s"Invalid message: $line")
            out.println("error")
        }
        line = in.readLine()
      }
    } finally {
      socket.close()
      println("Connection closed")
    }
  }
}
