package my.todos.http

import akka.actor.ActorRef
import akka.http.scaladsl.model.headers.{HttpOrigin, `Access-Control-Allow-Credentials`, `Access-Control-Allow-Headers`, `Access-Control-Allow-Origin`}
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ExceptionHandler, RejectionHandler, Route}
import akka.util.Timeout
import my.todos.models._
import my.todos.http.routes.{ProjectServiceRoute, TodoServiceRoute, UserServiceRoute}

import scala.collection.mutable
import scala.concurrent.ExecutionContext
import ch.megard.akka.http.cors.CorsDirectives._
import ch.megard.akka.http.cors.CorsSettings
import akka.http.scaladsl.model.HttpMethods._

import scala.collection.immutable.Seq

class RestService(todoService: ActorRef, userService: ActorRef, projectService: ActorRef)
                 (implicit executionContext: ExecutionContext, implicit val timeout: Timeout) {

  val userRouter = new UserServiceRoute(userService)
  val todoRouter = new TodoServiceRoute(todoService)
  val projectRouter = new ProjectServiceRoute(todoService, projectService)

  val rejectionHandler = corsRejectionHandler withFallback RejectionHandler.default

  val settings = CorsSettings.defaultSettings.copy(allowedMethods = Seq(GET, POST, OPTIONS, DELETE))

  // Your exception handler
  val exceptionHandler = ExceptionHandler {
    case _: UsernameExistsException =>
      extractUri { uri =>
        println(s"Request to $uri could not be handled normally")
        complete(HttpResponse(StatusCodes.InternalServerError, entity = "Username already exists"))
      }
    case e: Exception =>
      extractUri { uri =>
        println(s"Request to $uri could not be handled normally", e)
        complete(HttpResponse(StatusCodes.InternalServerError, entity = e.getLocalizedMessage))
      }
  }
  // Combining the two handlers only for convenience
  val handleErrors = handleRejections(rejectionHandler) & handleExceptions(exceptionHandler)

  val routes: Route =
    cors(settings) {
      handleErrors {
        userRouter.route ~
        todoRouter.route ~
        projectRouter.route
      }
    }
}
