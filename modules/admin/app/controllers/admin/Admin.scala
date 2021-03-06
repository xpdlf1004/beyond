package controllers.admin

import play.api.Play
import play.api.Mode
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.libs.json._
import play.core.PlayVersion
import play.modules.reactivemongo.MongoController
import play.modules.reactivemongo.json.collection.JSONCollection
import reactivemongo.api.Cursor
import scala.concurrent.Future
import scala.util.Properties

object Admin extends Controller with MongoController {
  private def collection: JSONCollection = db.collection[JSONCollection]("admin.password")

  private case class AdminUser(username: String, password: String)
  private implicit val adminUserFormat = Json.format[AdminUser]

  private class AuthenticatedRequest[A](val username: String, request: Request[A]) extends WrappedRequest[A](request)
  private object AuthenticatedAction extends ActionBuilder[AuthenticatedRequest] {
    def invokeBlock[A](request: Request[A], block: (AuthenticatedRequest[A]) => Future[Result]): Future[Result] = {
      request.session.get("username").map { username =>
        block(new AuthenticatedRequest(username, request))
      } getOrElse {
        Future.successful(Redirect(routes.Admin.login))
      }
    }
  }

  private val MinUsernameLength = 4
  private val MaxUsernameLength = 12

  private val loginForm = Form(
    mapping(
      "username" -> nonEmptyText(minLength = MinUsernameLength, maxLength = MaxUsernameLength),
      "password" -> nonEmptyText
    )(AdminUser.apply)(AdminUser.unapply)
  )

  private def serverInfo: Map[String, String] = {
    import Play.current
    Map(
      "OS Name" -> Properties.osName,
      "Play mode" -> Play.mode.toString,
      "PlayVersion current" -> PlayVersion.current,
      "PlayVersion sbtVersion" -> PlayVersion.sbtVersion,
      "PlayVersion scalaVersion" -> PlayVersion.scalaVersion
    )
  }

  def index: Action[AnyContent] = AuthenticatedAction { request =>
    val jsonServerInfo = Json.stringify(Json.toJson(serverInfo))
    Ok(views.html.admin_index(jsonServerInfo))
  }

  def login: Action[AnyContent] = Action { request =>
    request.session.get("username").map { username =>
      Redirect(routes.Admin.index)
    }.getOrElse {
      Ok(views.html.admin_login())
    }
  }

  def logout: Action[AnyContent] = Action {
    Redirect(routes.Admin.index).withNewSession
  }

  def userIndex: Action[AnyContent] = Action {
    Redirect(routes.Admin.userList)
  }

  def userList: Action[AnyContent] = AuthenticatedAction.async { request =>
    import play.api.libs.concurrent.Execution.Implicits._

    val cursor: Cursor[AdminUser] = collection.find(Json.obj()).cursor[AdminUser]
    val result: Future[List[AdminUser]] = cursor.collect[List]()
    result.map { users =>
      Ok(views.html.admin_user_list(Json.stringify(Json.toJson(users))))
    }
  }

  def doLogin: Action[AnyContent] = Action.async { implicit request =>
    loginForm.bindFromRequest.fold(
      formWithErrors => {
        // FIXME: Create an error type instead of passing String.
        val result = BadRequest(views.html.admin_login("Invalid username"))
        Future.successful(result)
      },
      loginData => {
        import play.api.libs.concurrent.Execution.Implicits._

        val cursor: Cursor[AdminUser] = collection.find(Json.obj("username" -> loginData.username)).cursor[AdminUser]
        val result: Future[List[AdminUser]] = cursor.collect[List]()
        result.map {
          case List() => BadRequest(views.html.admin_login("No such username"))
          case adminUser :: _ =>
            if (adminUser.password == loginData.password) {
              Redirect(routes.Admin.index).withSession("username" -> loginData.username)
            } else {
              BadRequest(views.html.admin_login("Invalid password"))
            }
        }
      }
    )
  }
}
