package my.todos

import com.mongodb.casbah.Imports._

object MongoFactory {
    // To directly connect to the default server localhost on port 27017
    val mongoClient: MongoClient = MongoClient()
    val collection = mongoClient("todos")("todos")

    def findAll = (for (record <- collection.find()) yield(convertDbObjectToTodo(record))).toList

    def findById(id: Int): Option[Todo] = {
        val opt = collection.findOne(MongoDBObject("id" -> id))
        opt match {
            case Some(value: DBObject) => Option(convertDbObjectToTodo(value))
            case None => None
        }
    }

    def createOrUpdate(todo: Todo) = {
        val query = MongoDBObject("id" -> todo.id)
        val update = $set("id" -> todo.id, "description" -> todo.description, "status" -> todo.status)
        val result = collection.update(query, update, upsert=true)

        println( "Number updated: " + result.getN )
        for (c <- collection.find) println(c)
        result
    }

    def delete(id: Int): Option[Todo] = {
        val opt = collection.findAndRemove(MongoDBObject("id" -> id))
        opt match {
            case Some(value: DBObject) => Option(convertDbObjectToTodo(value))
            case None => None
        }
    }

    def convertDbObjectToTodo(obj: DBObject): Todo = {
        val id = obj.getAs[Int]("id").getOrElse(0)
        val desc = obj.getAs[String]("description").getOrElse("?")
        val status = obj.getAs[Int]("status").getOrElse(0)
        Todo(id, desc, status)
    }
}