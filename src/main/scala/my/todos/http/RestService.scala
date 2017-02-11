package my.todos.http

import akka.actor.ActorRef
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout

import my.todos.models._
import my.todos.http.routes.{UserServiceRoute, TodoServiceRoute}
import my.todos.utils.CORS

import scala.collection.mutable
import scala.concurrent.ExecutionContext

class RestService(todoService: ActorRef, userService: ActorRef)
                 (implicit executionContext: ExecutionContext, implicit val timeout: Timeout)
  extends CORS {

  val users = mutable.Map[String, ApiUser]()
  val userRouter = new UserServiceRoute(userService, users)
  val todoRouter = new TodoServiceRoute(todoService, users)

  val routes: Route = cors {
    userRouter.route ~
    todoRouter.route
  }
}
