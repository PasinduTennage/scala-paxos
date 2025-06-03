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

  // todo this version does not implement pipelining yet!


  implicit val ec: ExecutionContext = ExecutionContext.global // global thread pool because this code uses multiple threads

  var numReplicas: Int = config.peers.length // parameter n in paxos

  var clientWriters: mutable.Map[Int, PrintWriter] = mutable.Map.empty // outgoing connection writers for clients

  var replicaWriters: mutable.Map[Int, PrintWriter] = mutable.Map.empty //outgoing connection writers for replicas

  var inputChannel = new SynchronousQueue[String]() // central buffer holding all the incoming messages

  var incomingClientBatches: mutable.Seq[ClientBatch] = ListBuffer.empty // client batches to be proposed later


  // initServer is the main execution point of Paxos server.

  def initServer(): Unit = {

    // start the server socket and handle new replica connections

    Future {

      println(s"initializing server ${name}")

      val replicaServer = new ServerSocket(port, 150, InetAddress.getByName("0.0.0.0"))

      println(s"Server started listening on port 0.0.0.0:$port")

      Future {
        println("server starting the main loop")
        this.run()
      }

      Future {
        println("Server started accepting new requests")
        while (true) {
          val socket = replicaServer.accept()
          Future {
            handle_server_socket(socket)
          }
        }
      }
    }

    // start proxy server and handle new client connections

    Future {
      println("initializing the proxy")

      val proxyServer = new ServerSocket(port + 1, 150, InetAddress.getByName("0.0.0.0"))

      println(s"Proxy started listening on port 0.0.0.0:${port + 1}")

      Future {
        println("Proxy started accepting new client requests")
        while (true) {
          val socket = proxyServer.accept()
          Future {
            handle_client_socket(socket)
          }
        }
      }

    }

    // connect to all replicas

    this.connectToReplicas()

    // start heartbeats

    this.startHeartBeats()
  }

  // periodically send the heart beats to all other replicas

  private def startHeartBeats(): Unit = {

  }

  // connect to all the replicas inSync

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

  // handle the new client connection

  private def handle_client_socket(socket: Socket): Unit = {

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

  // handler for new client batches

  private def handleClientBatch(m: ClientBatch): Unit = {
    println(s"client batch from ${m.senderId}")
    this.incomingClientBatches += m
  }


  // handle the new server connection

  def handle_server_socket(socket: Socket): Unit = {
    println("Server handling input connection")
    val in = new BufferedReader(new InputStreamReader(socket.getInputStream))
    var line: String = in.readLine()
    while (line != null) {
      this.inputChannel.put(line)
      line = in.readLine()
    }

  }

  // main Paxos event processing loop

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

  // paxos specific handlers

  private def handlePrepare(m: Prepare): Unit = {}

  private def handlePromise(m: Promise): Unit = {}

  private def handlePropose(m: Propose): Unit = {}

  private def handleAccept(m: Accept): Unit = {}

  private def handleDecide(m: Decide): Unit = {}

  private def handleFetchRequest(m: FetchRequest): Unit = {}

  private def handleFetchResponse(m: FetchResponse): Unit = {}

}
