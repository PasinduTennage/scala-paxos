import java.io.{BufferedReader, BufferedWriter, InputStreamReader, PrintWriter}
import java.net.Socket

object Client{
  def main(args: Array[String]):Unit = {
    val socket = new Socket("localhost",9999)
    val in = new BufferedReader(new InputStreamReader(socket.getInputStream))
    val out = new PrintWriter(socket.getOutputStream, true)
    var c = 1
    while (true) {
      out.println(c.toString)
      println(in.readLine())
      c = c+1
    }

  }
}