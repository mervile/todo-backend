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
    case CreateOrUpdateProjectUsers(projectUsers) => sender ! MongoFactory.createOrUpdateProjectUsers(projectUsers)
    case GetProjectsByUser(userId)                => sender ! MongoFactory.getProjectsByUser(userId)
    case GetProjectWithTodos(projectId)           => sender ! MongoFactory.getProjectWithTodos(projectId)
    case _                                        => println("huh?")
  }
}