package controllers

import java.util.Date

import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.scalalogging.LazyLogging
import controllers.HomeController._
import javax.inject._
import models.AppProtocol.{District, GetAllDistricts, GetAllRegions, Region}
import models.PatientProtocol.{ClientGroup, GetAllClientGroups}
import models.SimpleAuth
import models.UserProtocol._
import org.webjars.play.WebJarsUtil
import play.api.Configuration
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.I18nSupport
import play.api.libs.json.Json
import play.api.mvc._
import views.html.home

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}

object HomeController {
  case class LoginForm(
    login: String = "",
    password: String = ""
  )

  implicit val loginFormFormat = Json.format[LoginForm]

  implicit val playLoginForm: Form[LoginForm] = Form {
    mapping(
      "login" -> nonEmptyText,
      "password" -> nonEmptyText
    )(LoginForm.apply)(LoginForm.unapply)
  }

  val redirectUrl = controllers.routes.HomeController.index()
}

@Singleton
class HomeController @Inject()(val controllerComponents: ControllerComponents,
                               val configuration: Configuration,
                               @Named("user-manager") val userManager: ActorRef,
                               @Named("department-manager") val departmentManager: ActorRef,
                               @Named("patient-manager") val patientManager: ActorRef,
                               implicit val actorSystem: ActorSystem,
                               implicit val webJarsUtil: WebJarsUtil)
                              (implicit val ec: ExecutionContext)
  extends BaseController
    with I18nSupport
    with SimpleAuth
    with LazyLogging {

  implicit val defaultTimeout = Timeout(60.seconds)

  def index = Action { implicit request =>
    val result = auth {
      Ok(home.index())
    }

    if (result.header.status == UNAUTHORIZED) {
      Ok(home.login(playLoginForm))
    } else {
      result
    }
  }

  def login = Action { implicit request =>
    Ok(home.login(playLoginForm))
  }

  def loginPost = Action.async { implicit request =>
    playLoginForm.bindFromRequest.fold(
      errorForm => {
        logger.error(s"Form Error: $errorForm")
        Future.successful(Redirect(redirectUrl).flashing("error" -> "Login yoki parol noto'g'ri"))
      }, {
        case LoginForm(login, password) => checkLoginPassword(login, password)
          .recover {
            case error =>
              logger.error("error", error)
              Redirect(redirectUrl).flashing("error" -> "Login yoki parol noto'g'ri")
          }
      })
  }

  private def checkLoginPassword(login: String, password: String)(implicit request: RequestHeader) = {
    checkLogin(login, password).map {
      case Right((result, _)) => result
      case Left((result, _)) => result
    }
  }

  def checkLogin(login: String, password: String)(implicit request: RequestHeader): Future[Either[(Result, String), (Result, User)]]  = {
    def redirectWithParams(userAccount: Either[LoginAttemptsFailure, User]) = userAccount match {
      case Right(user) =>
        Future.successful(Redirect(redirectUrl).addingToSession(
          authInit(sessionKey, login, Some(sessionDuration)) ++
            authInit(roleSessionKey, user.roleCodes.getOrElse(""), Some(sessionDuration)): _*
        )).map(Right(_, user))

      case Left(loginFailure) =>
        (loginFailure match {
          case BlockedUser =>
            Future.successful("BlockedUser")
          case UserNotFound =>
            Future.successful("UserNotFound")
          case WrongPassword(failedAttemptsCount) =>
            if (failedAttemptsCount >= FailedAttemptsCountForBlockUser) {
              val blockedAt = Some(new Date)
              (userManager ? UpdateUsersBlockStatus(login, blockedAt)).mapTo[Unit].map { _ =>
                "UserHasBeenBlocked"
              }
            } else {
              Future.successful("WrongPassword")
            }
        }).map { failReason =>
          logger.info(s"LoginFailed, reason: $failReason")
          Left(Redirect(redirectUrl).flashing("error" -> failReason), failReason)
        }
    }

    (userManager ? CheckUserLogin(login, password)).mapTo[Either[LoginAttemptsFailure, User]].flatMap { result =>
      redirectWithParams(result)
    }.recover { case error =>
      logger.error(s"Error occurred during login.", error)
      sys.error("Error occurred during login to the dashboard")
    }
  }

  def logout = Action { implicit request: RequestHeader =>
    Redirect(controllers.routes.HomeController.login()).withSession(
      authClear(sessionKey, roleSessionKey)
    )
  }

  def redirectTo(menuName: String) = Action { implicit request => auth {
    menuName match {
      case "indicator" => Redirect(controllers.routes.IndicatorController.index())
      case "reception" => Redirect(controllers.routes.ReceptionController.index())
      case "cardIndex" => Redirect(controllers.routes.CardIndexController.index())
      case "patientReports" => Redirect(controllers.routes.ReportsController.patientIndex())
      case "checkUpReports" => Redirect(controllers.routes.ReportsController.checkUpIndex())
      case "settings" => Redirect(controllers.routes.SettingsController.index())
      case _ => Ok("Not implemented yet")
    }
  }}

  def getRegions = Action.async { implicit request => asyncAuth {
    (departmentManager ? GetAllRegions).mapTo[Seq[Region]].map { regions =>
      Ok(Json.toJson(regions))
    }
  }}

  def getDistricts = Action.async { implicit request => asyncAuth {
    (departmentManager ? GetAllDistricts).mapTo[Seq[District]].map { districts =>
      Ok(Json.toJson(districts))
    }
  }}

  def getClientGroups = Action.async { implicit request => asyncAuth {
    (patientManager ? GetAllClientGroups).mapTo[Seq[ClientGroup]].map { clientGroups =>
      Ok(Json.toJson(clientGroups))
    }.recover { case error =>
      logger.error("Error occurred during getting client groups", error)
      InternalServerError
    }
  }}

}