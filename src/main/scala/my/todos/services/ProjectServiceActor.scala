package my.todos.services

import akka.actor.{Actor, ActorLogging, Props}

import my.todos.models._
import my.todos.utils.MongoFactory

object ProjectServiceActor {
  def props(): Props = {
    Props(classOf[ProjectServiceActor])
  }
}

class ProjectServiceActor extends Actor with ActorLogging {
  def receive = {
    case CreateOrUpdateProject(project)           => sender ! MongoFactory.createOrUpdateProject(project)
    case DeleteProjectById(id)                    => sender ! MongoFactory.deleteProjectById(id)
    case GetProjectsByUser(userId)                => sender ! MongoFactory.getProjectsByUser(userId)
    case GetProjectWithTodosAndUsers(projectId)   => sender ! MongoFactory.getProjectWithTodosAndUsers(projectId)
    case AddProjectUser(projectId, userId)        => sender ! MongoFactory.addProjectUser(projectId, userId)
    case DeleteProjectUser(projectId, userId)     => sender ! MongoFactory.deleteProjectUser(projectId, userId)
    case _                                        => println("huh?")
  }
}