package my.todos.models

case class Project(id: String, title: String, description: String)

case class ProjectUsers(projectId: String, userId: String)

case class ProjectWithTodosAndUsers(project: Project, todos: List[Todo], users: List[User])

case class CreateOrUpdateProject(project: Project)

case class DeleteProjectById(id: String)

case class GetProjectsByUser(userId: String)

case class GetProjectWithTodosAndUsers(projectId: String)

case class AddProjectUser(projectId: String, userId: String)

case class DeleteProjectUser(projectId: String, userId: String)
