package paxos.server

import java.io.{BufferedReader, InputStreamReader, PrintWriter}
import java.net.{InetAddress, ServerSocket, Socket}
import scala.concurrent.{ExecutionContext, Future}
import upickle.default._
import java.util.concurrent.SynchronousQueue
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

import paxos.config._
import paxos.shared._
import java.time.{Duration, LocalDateTime}

class Server(
    val port: Int,
    val name: Int,
    val config: NetworkConfig,
    val replicaBathSize: Int,
    val viewTimeOut: Int,
    val logPath: String, //todo implement logging
    val debugLevel: Int, // todo implement debugging
    val pipeLineLength: Int // todo implement pipelining
) {

  implicit val ec: ExecutionContext =
    ExecutionContext.global // global thread pool because this code uses multiple threads

  var numReplicas: Int = config.peers.length // parameter n in paxos

  var clientWriters: mutable.Map[Int, PrintWriter] =
    mutable.Map.empty // outgoing connection writers for clients

  var replicaWriters: mutable.Map[Int, PrintWriter] =
    mutable.Map.empty // outgoing connection writers for replicas

  var inputChannel =
    new SynchronousQueue[
      String
    ]() // central buffer holding all the incoming messages

  var paxos_instance = new Paxos(
    numReplicas,
    this
  ) // paxos instance that will handle the protocol

  
  
  // initServer is the main execution point of Paxos server.

  def initServer(): Unit = {

    // start the server socket and handle new replica connections

    Future {

      println(s"initializing server ${name}")

      val replicaServer =
        new ServerSocket(port, 150, InetAddress.getByName("0.0.0.0"))

      println(s"Server started listening on port 0.0.0.0:$port")

      Future {
        println("server starting the main loop")
        this.run()
      }

      println("Server started accepting new requests")

      while (true) {
        val socket = replicaServer.accept()
        Future {
          handle_server_socket(socket)
        }
      }

    }

    // start proxy server and handle new client connections

    Future {
      println("initializing the proxy")

      val proxyServer =
        new ServerSocket(port + 1000, 150, InetAddress.getByName("0.0.0.0"))

      println(s"Proxy started listening on port 0.0.0.0:${port + 1000}")

      println("Proxy started accepting new client requests")

      while (true) {
        val socket = proxyServer.accept()
        Future {
          handle_client_socket(socket)
        }
      }
    }

    // connect to all replicas

    this.connectToReplicas()

    // start heartbeats

    this.startHeartBeats()
  }

  // connect to all the replicas

  private def connectToReplicas(): Unit = {
    this.config.peers.foreach { peer =>
      {
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
              Thread.sleep(1000)
          }
        }
      }
    }
    println("connected to all remote replicas")
  }

  // periodically send the heart beats to all other replicas

  private def startHeartBeats(): Unit = {

    while (true) {
      val msg = HeartBeat(name)
      val json = write[Message](msg)

      this.config.peers.foreach { peer =>
        {
          this.replicaWriters(peer.name).println(json)
        }
      }
      println(s"Sent heart beat to all replicas from ${name}")

      Thread.sleep(5000)
    }
  }

  // handle the new server connection -- put all incoming messages to buffer

  def handle_server_socket(socket: Socket): Unit = {
    println("Server handling input connection")

    val in = new BufferedReader(new InputStreamReader(socket.getInputStream))

    var line: String = in.readLine()
    while (line != null) {
      this.inputChannel.put(line)
      line = in.readLine()
    }

  }

  // main event processing loop

  def run(): Unit = {

    while (true) {

      val line = this.inputChannel.take()

      val msg = read[Message](line)

      msg match {

        case m: HeartBeat =>
          handleHeartBeat(m)

        case m: ClientBatch =>
          handleClientBatch(m)

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

  // handle the new client connection, and put all incoming messages to buffer

  private def handle_client_socket(socket: Socket): Unit = {

    println("Proxy handling client input connection")

    val in = new BufferedReader(new InputStreamReader(socket.getInputStream))

    var line: String = in.readLine()
    val msg = read[Message](line)
    msg match {
      case m: Id =>
        // client first sends its Id
        this.clientWriters(m.senderId) =
          new PrintWriter(socket.getOutputStream, true)
      case other =>
        throw new RuntimeException("Client should first send me the Id")
    }

    line = in.readLine()
    while (line != null) {
      this.inputChannel.put(line)
      line = in.readLine()
    }
  }

  // print heart beat details

  private def handleHeartBeat(m: HeartBeat): Unit = {
    println(s"Received heart beat from ${m.senderId}")
  }

  // handler for new client batches

  private def handleClientBatch(m: ClientBatch): Unit = {

    println(s"client batch from ${m.senderId}")

    this.paxos_instance.incomingClientBatches += m

    if (
      paxos_instance.is_proposing && Duration
        .between(LocalDateTime.now(), this.paxos_instance.last_proposed_time)
        .toMillis < this.viewTimeOut
    ) {
      // nothing
    } else {
      this.paxos_instance.is_proposing = false
      this.paxos_instance.send_prepare()

      println(
        s"Started a new prepare from ${this.name} at time ${LocalDateTime.now()}"
      )
    }
  }

  // paxos specific handlers

  private def handlePrepare(m: Prepare): Unit = {
    this.paxos_instance.handle_prepare(m)
  }

  private def handlePromise(m: Promise): Unit = {
    this.paxos_instance.handle_promise(m)
  }

  private def handlePropose(m: Propose): Unit = {
    this.paxos_instance.handle_propose(m)
  }

  private def handleAccept(m: Accept): Unit = {
    this.paxos_instance.handle_accept(m)
  }

  private def handleDecide(m: Decide): Unit = {
    this.paxos_instance.handle_decide(m)
  }

  private def handleFetchRequest(m: FetchRequest): Unit = {
    this.paxos_instance.handle_fetch_request(m)
  }

  private def handleFetchResponse(m: FetchResponse): Unit = {
    this.paxos_instance.handle_fetch_response(m)
  }

}
