package my.todos

import spray.json.DefaultJsonProtocol

case object Todos

case class Todo(id: Int, description: String, status: Int)

case class CreateorUpdateTodo(todo: Todo)

case class FindTodobyId(id: Int)

object Todo extends DefaultJsonProtocol {
    implicit val todoFormat = jsonFormat3(Todo.apply)
}
