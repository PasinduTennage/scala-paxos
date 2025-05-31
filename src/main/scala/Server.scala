import java.io.{BufferedReader, InputStreamReader, PrintWriter}
import java.net.ServerSocket

object Server{
  def main(args: Array[String]): Unit={
    val server = new ServerSocket(9999)
    println(s"server started on ${9999}")
    val socket = server.accept()

    val in = new BufferedReader(new InputStreamReader(socket.getInputStream))
    val out = new PrintWriter(socket.getOutputStream, true)

    var line: String = null

    while ({line = in.readLine();line!=null}){
      println(line)
      out.println(line)
    }

    socket.close()
    server.close()
  }
}