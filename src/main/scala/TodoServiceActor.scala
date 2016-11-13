import com.mongodb.casbah.Imports._
import akka.actor.Actor
import spray.json._
import DefaultJsonProtocol._

import Todo._

class TodoServiceActor extends Actor  {
    // To directly connect to the default server localhost on port 27017
    val mongoClient: MongoClient = MongoClient()
    val collection = mongoClient("todos")("todos")

    def receive = {
        case Todos => {
            val res = collection.find()
            sender ! (for (record <- res) yield(convertDbObjectToTodo(record))).toList
        }
        case _       => println("huh?")
    }

    def convertDbObjectToTodo(obj: DBObject): Todo = {
        val id = obj.getAs[Int]("id").getOrElse(0)
        val desc = obj.getAs[String]("description").getOrElse("?")
        val status = obj.getAs[Int]("status").getOrElse(0)
        Todo(id, desc, status)
    }
}