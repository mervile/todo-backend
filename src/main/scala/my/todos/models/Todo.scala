package my.todos.models

case class Todos(userId: String)

case class Todo(id: Int, description: String, status: Int, userId: String,
                projectId: String = "58c14984bb8a9fe5574b08ba")

case class CreateorUpdateTodo(todo: Todo)

case class FindTodobyId(id: Int)

case class DeleteTodobyId(id: Int)
