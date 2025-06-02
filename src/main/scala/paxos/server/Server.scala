package paxos.server

import paxos.config.NetworkConfig
import paxos.shared.ClientBatch

import java.io.{BufferedReader, InputStreamReader, PrintWriter}
import java.net.{ServerSocket, Socket}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}
import upickle.default._

import java.util.concurrent.SynchronousQueue
import scala.collection.mutable
import java.util.concurrent.locks.ReentrantLock
import scala.collection.mutable.ListBuffer

class Server(port: Int,
             name: Int,
             config: NetworkConfig,
             replicaBathSize: Int,
             replicaBatchTime: Int,
             viewTimeOut: Int,
             logPath: String,
             debugLevel: Int,
             pipeLineLength: Int) {
  implicit val ec: ExecutionContext = ExecutionContext.global
  var numReplicas: Int = config.peers.length

  var incomingClientReaders: mutable.Map[Int, BufferedReader] = mutable.Map.empty
  var incomingClientWriters: mutable.Map[Int, PrintWriter] = mutable.Map.empty
  var outgoingClientWriterLocks: mutable.Map[Int, ReentrantLock] = mutable.Map.empty


  var incomingReplicaReaders: mutable.Map[Int, BufferedReader] = mutable.Map.empty
  var incomingReplicaWriters: mutable.Map[Int, PrintWriter] = mutable.Map.empty
  var outgoingReplicaWriterLocks: mutable.Map[Int, ReentrantLock] = mutable.Map.empty


  var inputChannel = new SynchronousQueue[String]()

  var incomingClientBatches: mutable.Seq[ClientBatch] = ListBuffer[ClientBatch]()

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
