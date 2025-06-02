package paxos.server

import paxos.config.NetworkConfig
import paxos.shared.{Accept, ClientBatch, Decide, FetchRequest, FetchResponse, Message, Prepare, Promise, Propose}

import java.io.{BufferedReader, InputStreamReader, PrintWriter}
import java.net.{InetAddress, ServerSocket, Socket}
import scala.concurrent.{ExecutionContext, Future}
import upickle.default._

import java.util.concurrent.SynchronousQueue
import scala.collection.mutable
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

  var clientWriters: mutable.Map[Int, PrintWriter] = mutable.Map.empty

  var replicaWriters: mutable.Map[Int, PrintWriter] = mutable.Map.empty

  var inputChannel = new SynchronousQueue[String]()

  var incomingClientBatches: mutable.Seq[ClientBatch] = ListBuffer.empty

  def initServer(): Unit = {

    Future {

      println(s"initializing server ${name}")

      val tcpServer = new ServerSocket(port, 150, InetAddress.getByName("0.0.0.0"))

      println(s"Server started listening on port $port")

      Future {
        println("server starting the main loop")
        this.run()
      }

      Future {
        println("Server started accepting new requests")
        while (true) {
          val socket = tcpServer.accept()
          Future {
            handle(socket)
          }
        }
      }
    }


    Future {
      println("initializing the proxy")

      val proxyServer = new ServerSocket(port + 1, 150, InetAddress.getByName("0.0.0.0"))

      println(s"Proxy started listening on port ${port + 1}")

      Future {
        println("Proxy started accepting new client requests")
        while (true) {
          val socket = proxyServer.accept()
          Future {
            handle_client(socket)
          }
        }
      }

    }


    this.connectToReplicas()
  }

  private def handle_client(socket: Socket): Unit = {

    println("Proxy handling input connection")

    val in = new BufferedReader(new InputStreamReader(socket.getInputStream))

    var line: String = in.readLine()
    var setWriter = false

    while (line != null) {
      val msg = read[Message](line)
      msg match {
        case m: paxos.shared.ClientBatch => {
          if (!setWriter) {
            setWriter = true
            val client = m.senderId
            this.clientWriters(client) = new PrintWriter(socket.getOutputStream, true)
          }
          handleClientBatch(m)
        }
      }
      line = in.readLine()
    }
  }

  private def connectToReplicas(): Unit = {
    this.config.peers.foreach {
      peer => {
        var connected = false

        while (!connected) {
          try {
            val socket = new Socket(peer.ip, peer.port)
            val out = new PrintWriter(socket.getOutputStream, true)
            this.replicaWriters(peer.name) = out
            println(s"Connected to  $peer")
            connected = true
          } catch {
            case e: Exception =>
              Thread.sleep(100)
          }
        }
      }
    }
    println("connected to all peers")
  }

  def handle(socket: Socket): Unit = {
    println("Server handling input connection")
    val in = new BufferedReader(new InputStreamReader(socket.getInputStream))
    var line: String = in.readLine()
    while (line != null) {
      this.inputChannel.put(line)
      line = in.readLine()
    }

  }

  def run(): Unit = {

    while (true) {

      val line = this.inputChannel.take()

      val msg = read[Message](line)

      msg match {
        case m: paxos.shared.Prepare =>
          handlePrepare(m)

        case m: paxos.shared.Promise =>
          handlePromise(m)

        case m: paxos.shared.Propose =>
          handlePropose(m)

        case m: paxos.shared.Accept =>
          handleAccept(m)

        case m: paxos.shared.Decide =>
          handleDecide(m)

        case m: paxos.shared.FetchRequest =>
          handleFetchRequest(m)

        case m: paxos.shared.FetchResponse =>
          handleFetchResponse(m)
      }
    }
  }

  private def handlePrepare(m: Prepare): Unit = {}

  private def handlePromise(m: Promise): Unit = {}

  private def handlePropose(m: Propose): Unit = {}

  private def handleAccept(m: Accept): Unit = {}

  private def handleDecide(m: Decide): Unit = {}

  private def handleFetchRequest(m: FetchRequest): Unit = {}

  private def handleFetchResponse(m: FetchResponse): Unit = {}

  private def handleClientBatch(m: ClientBatch): Unit = {
    println(s"client batch from ${m.senderId}")
    this.incomingClientBatches += m
  }

}
