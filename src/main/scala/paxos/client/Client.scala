package paxos.client


import paxos.config.NetworkConfig
import paxos.shared.Message
import paxos.shared.ClientBatch
import paxos.shared.Id


import scala.concurrent.ExecutionContext
import scala.collection.mutable
import java.io.PrintWriter
import java.net.Socket
import scala.concurrent.Future
import java.util.concurrent.SynchronousQueue
import upickle.default._
import java.time.LocalDateTime


class Client(name: Int, config: NetworkConfig, duration: Int){
    var replicaWriters: mutable.Map[Int, PrintWriter] =
    mutable.Map.empty // outgoing connection writers for each replica

    implicit val ec: ExecutionContext =
    ExecutionContext.global // global thread pool because this code uses multiple threads

    var inputChannel = new SynchronousQueue[String]() // central buffer holding all the incoming client responses

    // sent time stamps
    var batchSentTime: mutable.Map[String, LocalDateTime] = mutable.Map.empty
    
    // received time stamps
    var batchReceivedTime: mutable.Map[String, LocalDateTime] = mutable.Map.empty

    var batchId: Int = 0

    def start(): Unit = {

        Future{
            this.run()
        }

        this.connectToReplicas(this.config)

        this.sendRequests()

        Thread.sleep(30000) // wait for 30 seconds to allow all requests to be sent and received

        // compute stats
        this.computeStats()
        
    }

    def run(): Unit = {

      while (true) {

        val line = this.inputChannel.take()

        val msg = read[Message](line)

        msg match {

          case m: ClientBatch =>
            // if m.id is not in this.batchReceivedTime, insert
            if (!this.batchReceivedTime.contains(m.id)) {
              this.batchReceivedTime(m.id) = LocalDateTime.now()
            }
        }
      }
    }

    def connectToReplicas(config: NetworkConfig): Unit = {
      config.peers.foreach { peer =>
        {
          var connected = false

          while (!connected) {
            try {
              val socket = new Socket(peer.ip, peer.port+1000)
              val out = new PrintWriter(socket.getOutputStream, true)
              println(s"Connected to replica ${peer.name} at ${peer.ip}:${peer.port}")
              // send id
              val msg = Id(this.name)
              val json = write[Message](msg)
              out.println(json)

              println(s"Sent ID to replica ${peer.name}")
              
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

    // send requests to replicas

    def sendRequests(): Unit={
      println(s"Client $name sending requests...")
      
      val endTime = LocalDateTime.now().plusSeconds(duration)
      
      while (LocalDateTime.now().isBefore(endTime)) {
        // create a client batch

        val id = s"$name:$batchId"
        val commands = List.tabulate(50)(i => s"command-$i-${System.currentTimeMillis()}")
        val batch = ClientBatch(id, this.name, commands)

        batchId += 1

        // send the batch to a random replica

        val replicaIndex = scala.util.Random.nextInt(this.config.peers.length)+1
        this.replicaWriters(replicaIndex).println(write[Message](batch))

        // update the sent time
        this.batchSentTime(id) = LocalDateTime.now()

        println(s"Client $name sent batch $id to replica $replicaIndex")

        // wait for a short time before sending the next batch
        Thread.sleep(100)
      }

    }

    private def handle_connection(in: java.io.BufferedReader): Unit = {
      println("Client handling input connection")

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


    // compute stats after the duration is over

    def computeStats(): Unit = {
      println(s"Client $name computing stats...")
      
      var totalReqests = 0
      var totalLatency:Long = 0

      this.batchSentTime.foreach { case (id, sentTime) =>
        if (this.batchReceivedTime.contains(id)) {
          val receivedTime = this.batchReceivedTime(id)
          val latency = java.time.Duration.between(sentTime, receivedTime).toMillis
          totalReqests += 50 // fixed number of commands per batch
          totalLatency += latency
        }else{
          println(s"Batch $id was sent but not received")
        }
      }

      // computer throughpt and average latency
      val throughput = totalReqests.toDouble / duration
      val averageLatency = if (totalReqests > 0) totalLatency.toDouble / totalReqests else 0.0

      println(s"Client $name stats:")
      println(s"Average latency: $averageLatency ms")
      println(s"Throughput: $throughput requests/sec")
    }

}