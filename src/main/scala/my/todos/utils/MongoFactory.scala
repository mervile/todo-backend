package my.todos.utils

import com.github.nscala_time.time.TypeImports
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.ValidBSONType.ObjectId
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
      case Some(value: DBObject) => Option(convertDbObjectToApiUser(value))
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
      "status" -> todo.status, "userId" -> todo.userId, "projectId" -> todo.projectId)
    val result = collection.update(query, update, upsert = true)

    println("Number updated: " + result.getN)
    for (c <- collection.find) println(c)
    result.getN
  }

  def createOrUpdateProject(project: Project): String = {
    if (project.id.trim() != "") {
      val query = MongoDBObject("id" -> project.id)
      val update = $set("title" -> project.title,
        "description" -> project.description)
      val result = projectCollection.update(query, update, upsert = true)
      println("Number of projects updated: " + result.getN)
      result.getUpsertedId.toString
    } else {
      val obj = MongoDBObject("title" -> project.title,
        "description" -> project.description)
      projectCollection.insert(obj)
      val id = obj.get("_id")
      println(s"Project created with id ${id}")
      id.toString
    }
  }

  def addProjectUser(projectId: String, userId: String) = {
    val query = MongoDBObject("projectId" -> projectId, "userId" -> userId)
    val update = $set("projectId" -> projectId, "userId" -> userId)
    val result = projectUsersCollection.update(query, update, true)

    println(s"Added user $userId to project $projectId")
    for (c <- projectUsersCollection.find) println(c)
    result.getN
  }

  def deleteProjectUser(projectId: String, userId: String) = {
    val query = MongoDBObject("projectId" -> projectId, "userId" -> userId)
    val result = projectUsersCollection.remove(query)

    println("Number removed: " + result.getN)
    for (c <- projectUsersCollection.find) println(c)
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

  def getProjectUsers(projectId: ObjectId): List[User] = {
    val result = projectUsersCollection.find(MongoDBObject("projectId" -> projectId.toString))
    val maybeUsers = for {
      obj <- result
      user <- userCollection.findOne(MongoDBObject("_id" -> new ObjectId(obj.getAs[String]("userId").get)))
    } yield user

    (for (obj <- maybeUsers) yield convertDbObjectToUser(obj)).toList
  }

  def getTodosByProject(projectId: ObjectId): List[Todo] = {
    val todos = collection.find(MongoDBObject("projectId" -> projectId.toString))
    (for (obj <- todos) yield convertDbObjectToTodo(obj)).toList
  }

  def getProjectsByUser(userId: String): List[ProjectWithTodosAndUsers] = {
    for {
      pu: ProjectUsers <- getProjectIdsByUser(userId)
      pwt: ProjectWithTodosAndUsers <- getProjectWithTodosAndUsers(pu.projectId)
    } yield pwt
  }

  def getProjectWithTodosAndUsers(projectId: String): Option[ProjectWithTodosAndUsers] = {
    val objId = new ObjectId(projectId)
    getProjectById(objId) match {
      case Some(project) => Option(ProjectWithTodosAndUsers(project,
        getTodosByProject(objId), getProjectUsers(objId)))
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

  def deleteProjectById(id: String): Option[Project] = {
    val opt = projectCollection.findAndRemove(MongoDBObject("_id" -> new ObjectId(id)))
    opt match {
      case Some(value: DBObject) => {
        val query = MongoDBObject("projectId" -> id)
        collection.remove(query)
        projectUsersCollection.remove(query)
        Option(convertDbObjectToProject(value))
      }
      case None => None
    }
  }

  def getUsers(): List[User] = (for (record <- userCollection.find()) yield (convertDbObjectToUser(record))).toList

  def convertDbObjectToApiUser(obj: DBObject): ApiUser = {
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

  def convertDbObjectToUser(obj: DBObject): User = {
    val username = obj.getAs[String]("username").getOrElse("?")
    val id = obj.getAs[ObjectId]("_id").getOrElse("?").toString
    User(id, username)
  }
}