package my.todos.services

import akka.actor.{Actor, ActorLogging, Props}

import my.todos.models._
import my.todos.utils.MongoFactory

object TodoServiceActor {
  def props(): Props = {
    Props(classOf[TodoServiceActor])
  }
}

class TodoServiceActor extends Actor with ActorLogging {
  def receive = {
    case Todos(userId)             => sender ! MongoFactory.findAllByUser(userId)
    case CreateorUpdateTodo(todo)  => sender ! MongoFactory.createOrUpdate(todo)
    case FindTodobyId(id)          => sender ! MongoFactory.findById(id)
    case DeleteTodobyId(id)        => sender ! MongoFactory.delete(id)
    case _                         => println("huh?")
  }
}