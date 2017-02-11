package my.todos.utils

import com.mongodb.casbah.Imports._
import my.todos.models.{ApiUser, Todo}

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

  def createUser(user: ApiUser): Unit = {
    val query = MongoDBObject("username" -> user.username,
      "hashedPassword" -> user.hashedPassword)
    val result = userCollection.insert(query)

    println("Created: " + result.getN)
    for (c <- userCollection.find) println(c)
    result.getN
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
    result.getN
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
    ApiUser(username, hashedPassword, Some(id))
  }

  def convertDbObjectToTodo(obj: DBObject): Todo = {
    // TODO use id given by db
    val id = obj.getAs[Int]("id").getOrElse(0)
    val desc = obj.getAs[String]("description").getOrElse("?")
    val status = obj.getAs[Int]("status").getOrElse(0)
    val userId = obj.getAs[String]("userId").getOrElse("?")
    Todo(id, desc, status, userId)
  }
}