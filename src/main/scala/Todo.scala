import spray.json.DefaultJsonProtocol

case class Todo(id: Int, description: String, status: Int)

object Todo extends DefaultJsonProtocol {
    implicit val todoFormat = jsonFormat3(Todo.apply)
}
