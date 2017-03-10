package my.todos.http.routes

import akka.actor.ActorRef
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.ExecutionContext
import my.todos.models._
import my.todos.utils.TodosJSONSupport
import my.todos.utils.UserPassAuthenticator.myUserPassAuthenticator

class TodoServiceRoute(val todoService: ActorRef)
                      (implicit executionContext: ExecutionContext, implicit val timeout: Timeout)
  extends TodosJSONSupport{

  val route: Route =
    pathPrefix("api") {
      authenticateOAuth2(realm = "secure site", myUserPassAuthenticator) { user =>
        path("todos") {
          pathEndOrSingleSlash {
            get {
              onSuccess((todoService ? Todos(user.id.get)).mapTo[List[Todo]]) { todos =>
                complete(todos)
              }
            }
          }
        } ~
        path("todo") {
          pathEndOrSingleSlash {
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
}
