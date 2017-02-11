package my.todos.services

import akka.actor.{Actor, ActorLogging, Props}

import my.todos.models._
import my.todos.utils.MongoFactory

object UserServiceActor {
  def props(): Props = {
    Props(classOf[UserServiceActor])
  }
}

class UserServiceActor extends Actor with ActorLogging {
  def receive = {
    case FindUser(username)        => sender ! MongoFactory.findUser(username)
    case CreateUser(user)          => sender ! MongoFactory.createUser(user)
    case _                         => println("huh?")
  }
}