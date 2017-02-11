package my.todos

import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.util.Timeout
import my.todos.http.RestService
import my.todos.services.{TodoServiceActor, UserServiceActor}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object WebService extends App {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = system.dispatcher
  implicit val timeout = Timeout(10.seconds)
  implicit val log: LoggingAdapter = Logging(system, getClass)

  val userService = system.actorOf(UserServiceActor.props(),"userService")
  val todoService = system.actorOf(TodoServiceActor.props(),"todoService")
  val restService = new RestService(todoService, userService)

  Http().bindAndHandle(restService.routes, "localhost", 8080)
  println(s"Waiting for requests at my.todos.http://localhost:8080/...\nType re-stop to terminate")
}