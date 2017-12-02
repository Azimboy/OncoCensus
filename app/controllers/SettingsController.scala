package controllers

import java.util.Date
import javax.inject.{Inject, Named, Singleton}

import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.scalalogging.LazyLogging
import controllers.SettingsController._
import models.AppProtocol.{CreateDepartment, DeleteDepartment, Department, District, GetAllRegions, GetDepartmentsReport, GetDistrictsByRegionId, Region, UpdateDepartment}
import models.UserAccountProtocol.{AddUserAccount, GetAllUserAccounts, UserAccount}
import models.utils.CieloConfigUtil._
import org.webjars.play.WebJarsUtil
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc.{BaseController, ControllerComponents}
import views.html.settings

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

object SettingsController {
	case class UserAccountWeb(
	  login: String,
	  firstName: Option[String] = None,
	  lastName: Option[String] = None,
	  middleName: Option[String] = None,
	  departmentId: Option[Int] = None,
	  roleCodes: Option[String] = None,
	  email: Option[String] = None,
	  phoneNumber: Option[String] = None,
	)

	case class DepartmentWeb(
		districtId: Int,
		name: String
	)

	implicit val userAccountWebFormat = Json.format[UserAccountWeb]
	implicit val departmentWebFormat = Json.format[DepartmentWeb]
}

@Singleton
class SettingsController @Inject()(val controllerComponents: ControllerComponents,
                                   val configuration: Configuration,
                                   @Named("user-account-manager") val userAccountManager: ActorRef,
                                   @Named("department-manager") val departmentManager: ActorRef,
                                   implicit val webJarsUtil: WebJarsUtil,
                                   implicit val actorSystem: ActorSystem
																	)(implicit val ec: ExecutionContext)
	extends BaseController with LazyLogging {

	implicit val defaultTimeout = Timeout(60.seconds)

	implicit val currentConfig = getWebServerConfig(configuration)
//	val userAccountManager = getActorSelFromConfig("actor-path.user-account-manager")

	def index = Action {
		Ok(settings.index())
	}

	def getUsers = Action.async { implicit request =>
		(userAccountManager ? GetAllUserAccounts).mapTo[Seq[UserAccount]].map { users =>
			Ok(Json.toJson(users))
		}
	}

	def getRegions() = Action.async { implicit request =>
		(userAccountManager ? GetAllRegions).mapTo[Seq[Region]].map { regions =>
			Ok(Json.toJson(regions))
		}
	}

	def getDistrictsByRegionId(regionId: Int) = Action.async { implicit request =>
		(userAccountManager ? GetDistrictsByRegionId(regionId)).mapTo[Seq[District]].map { districts =>
			Ok(Json.toJson(districts))
		}
	}

	def createUser = Action.async(parse.json[UserAccountWeb]) { implicit request =>
		val userAccountWeb = request.body

		val newUserAccount = UserAccount(
			login = userAccountWeb.login,
			passwordHash = "123",
			createdAt = Some(new Date),
			firstName = userAccountWeb.firstName,
			lastName = userAccountWeb.lastName,
			middleName = userAccountWeb.middleName,
			departmentId = userAccountWeb.departmentId,
			roleCodes = userAccountWeb.roleCodes,
			email = userAccountWeb.email,
			phoneNumber = userAccountWeb.phoneNumber
		)

		(userAccountManager ? AddUserAccount(newUserAccount)).mapTo[Int].map { id =>
			Ok(Json.toJson(id))
		}.recover { case error =>
			logger.error("Add user error", error)
			InternalServerError
		}
	}
// TODO fix User update
	def updateUser(id: Int) = Action.async(parse.json[UserAccountWeb]) { implicit request =>
		val userAccountWeb = request.body

		val newUserAccount = UserAccount(
			login = userAccountWeb.login,
			passwordHash = "123",
			createdAt = Some(new Date),
			firstName = userAccountWeb.firstName,
			lastName = userAccountWeb.lastName,
			middleName = userAccountWeb.middleName,
			departmentId = userAccountWeb.departmentId,
			roleCodes = userAccountWeb.roleCodes,
			email = userAccountWeb.email,
			phoneNumber = userAccountWeb.phoneNumber
		)

		(userAccountManager ? AddUserAccount(newUserAccount)).mapTo[Int].map { id =>
			Ok(Json.toJson(id))
		}.recover { case error =>
			logger.error("Add user error", error)
			InternalServerError
		}
	}

	def getDepartments = Action.async { implicit request =>
		(departmentManager ? GetDepartmentsReport).mapTo[Seq[Department]].map { reports =>
			Ok(Json.toJson(reports))
		}.recover { case error =>
			logger.error("Deps", error)
			InternalServerError
		}
	}

	def createDepartment = Action.async(parse.json[Department]) { implicit request =>
		(departmentManager ? CreateDepartment(request.body)).mapTo[Int].map { id =>
			Ok(Json.toJson(id))
		}.recover { case error =>
			logger.error("Create department error", error)
			InternalServerError
		}
	}

	def updateDepartment(id: Int) = Action.async(parse.json[Department]) { implicit request =>
		(departmentManager ? UpdateDepartment(request.body)).mapTo[Int].map { id =>
			Ok(Json.toJson(id))
		}.recover { case error =>
			logger.error("Update department error", error)
			InternalServerError
		}
	}

	def deleteDepartment(id: Int) = Action.async { implicit request =>
		(departmentManager ? DeleteDepartment(id)).mapTo[Int].map { id =>
			Ok(Json.toJson(id))
		}.recover { case error =>
			logger.error("Update department error", error)
			InternalServerError
		}
	}
}