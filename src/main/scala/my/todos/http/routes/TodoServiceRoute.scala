package my.todos.http.routes

import akka.actor.ActorRef
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.Credentials
import akka.pattern.ask
import akka.util.Timeout
import pdi.jwt.{Jwt, JwtAlgorithm}

import scala.collection.mutable
import scala.concurrent.ExecutionContext

import my.todos.models._

class TodoServiceRoute(val todoService: ActorRef, val users: mutable.Map[String, ApiUser])
                      (implicit executionContext: ExecutionContext, implicit val timeout: Timeout)
  extends TodosJSONSupport{

  def myUserPassAuthenticator(credentials: Credentials): Option[ApiUser] =
    credentials match {
      case p @ Credentials.Provided(id) => {
        if (Jwt.isValid(id, "secretKey", Seq(JwtAlgorithm.HS256))) users get id
        else None
      }
      case _ => None
    }

  val route: Route =
    pathPrefix("api") {
      authenticateOAuth2(realm = "secure site", myUserPassAuthenticator) { user =>
        path("todos") {
          get {
            onSuccess((todoService ? Todos(user.id.get)).mapTo[List[Todo]]) { todos =>
              complete(todos)
            }
          }
        } ~
        path("todo") {
          post {
            entity(as[Todo]) { todo =>
              var newTodo = todo
              if (todo.id == -1) {
                newTodo = Todo(scala.util.Random.nextInt, todo.description, todo.status, user.id.get)
              }
              onSuccess((todoService ? CreateorUpdateTodo(newTodo))) { _ =>
                complete(StatusCodes.Created, newTodo)
              }
            }
          } ~
          delete {
            parameters('id.as[Int]) { (id) =>
              onSuccess((todoService ? DeleteTodobyId(id)).mapTo[Option[Todo]]) { maybeDeleted =>
                maybeDeleted match {
                  case Some(deleted) => complete(StatusCodes.OK, deleted)
                  case None => complete(StatusCodes.NotFound, s"Couldn't find todo by id ${id}")
                }
              }
            }
          }
        }
      }
    }
}
