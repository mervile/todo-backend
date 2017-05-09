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
              onSuccess(future.mapTo[List[ProjectWithTodosAndUsers]]) { res =>
                complete(res)
              }
            }
          }
        } ~
        path("project") {
          pathEndOrSingleSlash {
            post {
              entity(as[ProjectWithTodosAndUsers]) { pwts =>
                if (pwts.project.id.trim().length > 0) {
                  // Update
                  onSuccess((projectService ? UpdateProject(pwts.project))) { id =>
                    onSuccess((projectService ? GetProjectUsers(pwts.project.id))) { res =>
                      val users:List[User] = res.asInstanceOf[List[User]]
                      val toRemove = users.filterNot(pwts.users.contains(_))
                      val toAdd = pwts.users.filterNot(users.contains(_))
                      toRemove.map((usr:User) => projectService ! DeleteProjectUser(pwts.project.id, usr.id))
                      toAdd.map((usr:User) => projectService ! AddProjectUser(pwts.project.id, usr.id))
                      complete(StatusCodes.OK, pwts)
                    }
                  }
                } else {
                  // Create
                  onSuccess((projectService ? CreateProject(pwts.project))) { id =>
                    onSuccess(projectService ? AddProjectUser(id.toString, user.id.get)) { _ =>
                      pwts.users.map((usr:User) => projectService ! AddProjectUser(id.toString, usr.id))
                      val newProject: Project = Project(id.toString, pwts.project.title, pwts.project.description)
                      val projectWithTodosAndUsers = ProjectWithTodosAndUsers(newProject, pwts.todos, pwts.users)
                      complete(StatusCodes.Created, projectWithTodosAndUsers)
                    }
                  }
                }
              }
            } ~
            delete {
              parameters('id.as[String]) { (id) =>
                onSuccess((projectService ? DeleteProjectById(id)).mapTo[Option[Project]]) { maybeDeleted =>
                  maybeDeleted match {
                    case Some(deleted) => complete(StatusCodes.OK, deleted)
                    case None => complete(StatusCodes.NotFound, s"Couldn't find project by id ${id}")
                  }
                }
              }
            }
          }
        }
      }
    }
}
