package my.todos

import spray.json.DefaultJsonProtocol

case class Todos(userId: String)

case class Todo(id: Int, description: String, status: Int, userId: String)

case class CreateorUpdateTodo(todo: Todo)

case class FindTodobyId(id: Int)

case class DeleteTodobyId(id: Int)

object Todo extends DefaultJsonProtocol {
    implicit val todoFormat = jsonFormat4(Todo.apply)
}
