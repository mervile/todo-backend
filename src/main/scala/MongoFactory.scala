package my.todos

import com.mongodb.casbah.Imports._

object MongoFactory {
  // To directly connect to the default server localhost on port 27017
  val mongoClient: MongoClient = MongoClient()
  val collection = mongoClient("todos")("todos")
  val userCollection = mongoClient("todos")("users")

  def findUser(username: String): Option[ApiUser] = {
    val opt = userCollection.findOne(MongoDBObject("username" -> username))
    opt match {
      case Some(value: DBObject) => Option(convertDbObjectToUser(value))
      case None => None
    }
  }

  def findAll = (for (record <- collection.find()) yield (convertDbObjectToTodo(record))).toList

  def findAllByUser(userId: String) = (
    for (record <- collection.find(MongoDBObject("userId" -> userId))) yield (convertDbObjectToTodo(record))).toList

  def findById(id: Int): Option[Todo] = {
    val opt = collection.findOne(MongoDBObject("id" -> id))
    opt match {
      case Some(value: DBObject) => Option(convertDbObjectToTodo(value))
      case None => None
    }
  }

  def createOrUpdate(todo: Todo) = {
    val query = MongoDBObject("id" -> todo.id)
    val update = $set("id" -> todo.id, "description" -> todo.description,
      "status" -> todo.status, "userId" -> todo.userId)
    val result = collection.update(query, update, upsert = true)

    println("Number updated: " + result.getN)
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

  def convertDbObjectToUser(obj: DBObject): ApiUser = {
    val username = obj.getAs[String]("username").getOrElse("")
    val hashedPassword = obj.getAs[String]("hashedPassword")
    val id = obj.getAs[ObjectId]("_id").getOrElse("-1").toString()
    ApiUser(username, hashedPassword, id)
  }

  def convertDbObjectToTodo(obj: DBObject): Todo = {
    val id = obj.getAs[Int]("id").getOrElse(0)
    val desc = obj.getAs[String]("description").getOrElse("?")
    val status = obj.getAs[Int]("status").getOrElse(0)
    val userId = obj.getAs[String]("userId").getOrElse("?")
    Todo(id, desc, status, userId)
  }
}