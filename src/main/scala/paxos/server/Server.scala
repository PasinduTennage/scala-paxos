package paxos.server

import java.io.{BufferedReader, InputStreamReader, PrintWriter}
import java.net.{ServerSocket, Socket}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class Server(port: Int) {
  implicit val ec: ExecutionContext = ExecutionContext.global

  def start(): Unit = {
    val server = new ServerSocket(this.port)
    while (true) {
      val socket = server.accept()
      Future {
        handle(socket)
      }.onComplete {
        case Failure(e) => println("failed")
        case Success(_) => println("success")
      }
    }
  }

  def handle(socket: Socket): Unit = {
    val in = new BufferedReader(new InputStreamReader(socket.getInputStream))
    val out = new PrintWriter(socket.getOutputStream, true);
    var line: String = ""

    try {
      while (line != null) {
        line = in.readLine()
        out.println(line)
      }
    } finally {
      socket.close()
    }
  }
}
