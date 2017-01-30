package my.todos

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.util.Timeout

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.io.StdIn

class WebService(implicit val system: ActorSystem,
                 implicit val materializer: ActorMaterializer,
                 implicit val executionContext: ExecutionContext) extends RestService {
  def startServer(host: String, port: Int) = {
    Http().bindAndHandle(route, host, port)
    println(s"Waiting for requests at http://$host:$port/...\nType re-stop to terminate")
  }
}

object WebService {

  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("todo-backend")
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher
    implicit val timeout = Timeout(10.seconds)

    val server = new WebService()
    server.startServer("localhost",8080)
  }
}