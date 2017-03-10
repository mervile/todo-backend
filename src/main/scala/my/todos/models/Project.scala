package my.todos.models

case class Project(id: String, title: String, description: String)

case class ProjectUsers(projectId: String, userId: String)

case class ProjectWithTodos(project: Project, todos: List[Todo])

case class CreateOrUpdateProject(project: Project)

case class CreateOrUpdateProjectUsers(projectUsers: ProjectUsers)

case class GetProjectsByUser(userId: String)

case class GetProjectWithTodos(projectId: String)
