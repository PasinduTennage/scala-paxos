package paxos.client


import scala.concurrent.ExecutionContext
import scala.collection.mutable
import java.io.PrintWriter
import java.net.Socket
import paxos.config.NetworkConfig
import scala.concurrent.Future
import java.util.concurrent.SynchronousQueue
import paxos.shared.Message
import upickle.default._
import paxos.shared.ClientBatch

class Client(name: Int, config: NetworkConfig){
    var replicaWriters: mutable.Map[Int, PrintWriter] =
    mutable.Map.empty // outgoing connection writers for replicas

    implicit val ec: ExecutionContext =
    ExecutionContext.global // global thread pool because this code uses multiple threads

    var inputChannel =
    new SynchronousQueue[
      String
    ]() // central buffer holding all the incoming messages


    def start(): Unit = {

        Future{
            this.run()
        }

        this.connectToReplicas(this.config)
        
    }
    
    def connectToReplicas(config: NetworkConfig): Unit = {
    config.peers.foreach { peer =>
      {
        var connected = false

        while (!connected) {
          try {
            val socket = new Socket(peer.ip, peer.port)
            val out = new PrintWriter(socket.getOutputStream, true)
            this.replicaWriters(peer.name) = out
            val in = new java.io.BufferedReader(
              new java.io.InputStreamReader(socket.getInputStream)
            )
            Future{
              this.handle_connection(in)
            }

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

  private def handle_connection(in: java.io.BufferedReader): Unit = {
    while (true) {
      try {
        val line = in.readLine()
        if (line != null) {
          this.inputChannel.put(line)
        } else {
          println("Connection closed by server")
          return
        }
      } catch {
        case e: Exception =>
          println(s"Error reading from connection: ${e.getMessage}")
          return
      }
    }
  }

  // main event processing loop

  def run(): Unit = {

    while (true) {

      val line = this.inputChannel.take()

      val msg = read[Message](line)

      msg match {

        case m: ClientBatch =>
          // TODO

      }
    }
  }



}