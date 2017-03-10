package my.todos.utils

import com.mongodb.casbah.Imports._
import my.todos.models._

object MongoFactory {
  // To directly connect to the default server localhost on port 27017
  val mongoClient: MongoClient = MongoClient()
  val collection = mongoClient("todos")("todos")
  val userCollection = mongoClient("todos")("users")
  val projectCollection = mongoClient("todos")("projects")
  val projectUsersCollection = mongoClient("todos")("projectusers")

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

  def createOrUpdateProject(project: Project) = {
    val query = MongoDBObject("id" -> project.id)
    val update = $set("id" -> project.id, "title" -> project.title,
      "description" -> project.description)
    val result = projectCollection.update(query, update, upsert = true)

    println("Number of projects updated: " + result.getN)
    for (c <- projectCollection.find) println(c)
    result.getN
  }

  // TODO only add or delete
  def createOrUpdateProjectUsers(projectUsers: ProjectUsers) = {
    val query = MongoDBObject("projectId" -> projectUsers.projectId)
    val update = $set("projectId" -> projectUsers.projectId, "userId" -> projectUsers.userId)
    val result = projectUsersCollection.update(query, update, upsert = true)

    println("Number of projectusers updated: " + result.getN)
    for (c <- projectUsersCollection.find) println(c)
    result.getN
  }

  def getProjectIdsByUser(userId: String): List[ProjectUsers] = {
    val projectIds = projectUsersCollection.find(MongoDBObject("userId" -> userId))
    (for (obj <- projectIds) yield convertDbObjectToProjectUser(obj)).toList
  }

  def getProjectById(projectId: ObjectId): Option[Project] = {
    val opt = projectCollection.findOne(MongoDBObject("_id" -> projectId))
    opt match {
      case Some(value: DBObject) => {
        Option(convertDbObjectToProject(value))
      }
      case None => None
    }
  }

  def getTodosByProject(projectId: ObjectId): List[Todo] = {
    val todos = collection.find(MongoDBObject("projectId" -> projectId.toString))
    (for (obj <- todos) yield convertDbObjectToTodo(obj)).toList
  }

  def getProjectsByUser(userId: String): List[ProjectWithTodos] = {
    for {
      pu: ProjectUsers <- getProjectIdsByUser(userId)
      pwt: ProjectWithTodos <- getProjectWithTodos(pu.projectId)
    } yield pwt
  }

  def getProjectWithTodos(projectId: String): Option[ProjectWithTodos] = {
    val objId = new ObjectId(projectId)
    getProjectById(objId) match {
      case Some(project) => Option(ProjectWithTodos(project, getTodosByProject(objId)))
      case None          => None
    }
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
    val projectId = obj.getAs[String]("projectId").getOrElse("58c14984bb8a9fe5574b08ba").toString
    Todo(id, desc, status, userId, projectId)
  }

  def convertDbObjectToProject(obj: DBObject): Project = {
    val id = obj.getAs[ObjectId]("_id").getOrElse("58c14984bb8a9fe5574b08ba").toString
    val title = obj.getAs[String]("title").getOrElse("?")
    val description = obj.getAs[String]("description").getOrElse("?")
    Project(id, title, description)
  }

  def convertDbObjectToProjectUser(obj: DBObject): ProjectUsers = {
    val projectId = obj.getAs[String]("projectId").getOrElse("58c14984bb8a9fe5574b08ba")
    val userId = obj.getAs[String]("userId").getOrElse("-1")
    ProjectUsers(projectId, userId)
  }
}