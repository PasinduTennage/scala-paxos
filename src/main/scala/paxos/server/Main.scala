package paxos.server

import java.io.{BufferedReader, InputStreamReader, PrintWriter}
import java.net.{ServerSocket, Socket}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}


object Main {
  // Use global execution context (or define your own thread pool)
  implicit val ec: ExecutionContext = ExecutionContext.global

  def main(args: Array[String]): Unit = {
    val server = new ServerSocket(9999)
    while (true) {
      val socket = server.accept()
      Future {
        handle(socket)
      }.onComplete {
        case Failure(e) => println("error")
        case Success(_) => println("success")
      }
    }
  }

  def handle(socket: Socket): Unit = {
    val in = new BufferedReader(new InputStreamReader(socket.getInputStream))
    val out = new PrintWriter(socket.getOutputStream, true)
    var running = true
    while (running) {
      val s = in.readLine()
      if (s == null) {
        running = false
      } else {
        out.println(s)
      }
    }

    socket.close()
  }
}
