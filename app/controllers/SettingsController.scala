package controllers

import java.util.Date

import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.scalalogging.LazyLogging
import controllers.SettingsController._
import javax.inject.{Inject, Named, Singleton}
import models.AppProtocol.{CreateDepartment, DeleteDepartment, Department, GetDepartmentsReport, UpdateDepartment}
import models.SimpleAuth
import models.UserProtocol.{GetAllUsers, ModifyUser, User, roles}
import models.utils.StringUtils.createHash
import org.webjars.play.WebJarsUtil
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc.{BaseController, ControllerComponents}
import views.html.settings

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

object SettingsController {
	case class UserWeb(
		id: Option[Int],
		createdAt: Option[Date],
	  login: String,
		password: String,
	  firstName: Option[String] = None,
	  lastName: Option[String] = None,
	  middleName: Option[String] = None,
	  departmentId: Option[Int] = None,
	  roleCodes: Option[String] = None,
	  email: Option[String] = None,
	  phoneNumber: Option[String] = None
	)

	case class DepartmentWeb(
		districtId: Int,
		name: String
	)

	implicit val userWebFormat = Json.format[UserWeb]
	implicit val departmentWebFormat = Json.format[DepartmentWeb]
}

@Singleton
class SettingsController @Inject()(val controllerComponents: ControllerComponents,
                                   val configuration: Configuration,
                                   @Named("user-manager") val userManager: ActorRef,
                                   @Named("department-manager") val departmentManager: ActorRef,
																	 implicit val actorSystem: ActorSystem,
																	 implicit val webJarsUtil: WebJarsUtil)
																	(implicit val ec: ExecutionContext)
	extends BaseController
		with SimpleAuth
		with LazyLogging {

	implicit val defaultTimeout = Timeout(60.seconds)

	def index = Action { implicit request => auth {
		Ok(settings.index())
	}}

	def getUsers = Action.async { implicit request => asyncAuth {
		(userManager ? GetAllUsers).mapTo[Seq[User]].map { users =>
			Ok(Json.toJson(users.map(_.copy(passwordHash = ""))))
		}
	}}

	def modifyUser = Action.async(parse.json[UserWeb]) { implicit request => asyncAuth {
		val userWeb = request.body

		val newUser = User(
			id = userWeb.id,
			login = userWeb.login,
			passwordHash = createHash(userWeb.password),
			createdAt = Some(userWeb.createdAt.getOrElse(new Date)),
			firstName = userWeb.firstName,
			lastName = userWeb.lastName,
			middleName = userWeb.middleName,
			departmentId = userWeb.departmentId,
			roleCodes = userWeb.roleCodes,
			email = userWeb.email,
			phoneNumber = userWeb.phoneNumber
		)

		(userManager ? ModifyUser(newUser)).mapTo[Int].map { id =>
			Ok(Json.toJson(id))
		}.recover { case error =>
			logger.error("Add user error", error)
			InternalServerError
		}
	}}

	def getDepartments = Action.async { implicit request => asyncAuth {
		(departmentManager ? GetDepartmentsReport).mapTo[Seq[Department]].map { reports =>
			Ok(Json.toJson(reports))
		}.recover { case error =>
			logger.error("Deps", error)
			InternalServerError
		}
	}}

	def createDepartment = Action.async(parse.json[Department]) { implicit request => asyncAuth {
		(departmentManager ? CreateDepartment(request.body)).mapTo[Int].map { id =>
			Ok(Json.toJson(id))
		}.recover { case error =>
			logger.error("Create department error", error)
			InternalServerError
		}
	}}

	def updateDepartment(id: Int) = Action.async(parse.json[Department]) { implicit request => asyncAuth {
		(departmentManager ? UpdateDepartment(request.body)).mapTo[Int].map { id =>
			Ok(Json.toJson(id))
		}.recover { case error =>
			logger.error("Update department error", error)
			InternalServerError
		}
	}}

	def deleteDepartment(id: Int) = Action.async { implicit request => asyncAuth {
		(departmentManager ? DeleteDepartment(id)).mapTo[Int].map { id =>
			Ok(Json.toJson(id))
		}.recover { case error =>
			logger.error("Update department error", error)
			InternalServerError
		}
	}}

	def getRoles() = Action { implicit request => auth {
		Ok(Json.toJson(roles))
	}}
}