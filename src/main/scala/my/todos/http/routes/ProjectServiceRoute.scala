package my.todos.http.routes

import akka.actor.ActorRef
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.ExecutionContext
import my.todos.models._
import my.todos.utils.TodosJSONSupport
import my.todos.utils.UserPassAuthenticator.myUserPassAuthenticator

class ProjectServiceRoute(val todoService: ActorRef, val projectService: ActorRef)
                      (implicit executionContext: ExecutionContext, implicit val timeout: Timeout)
  extends TodosJSONSupport {

  val route: Route =
    pathPrefix("api") {
      authenticateOAuth2(realm = "secure site", myUserPassAuthenticator) { user =>
        path("projects") {
          pathEndOrSingleSlash {
            get {
              val future = projectService ? GetProjectsByUser(user.id.get)
              onSuccess(future.mapTo[List[ProjectWithTodos]]) { res =>
                complete(res)
              }
            }
          }
        }
      }
    }
}
