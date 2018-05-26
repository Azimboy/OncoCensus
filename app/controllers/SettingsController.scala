package controllers

import java.util.Date

import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.scalalogging.LazyLogging
import controllers.SettingsController._
import javax.inject.{Inject, Named, Singleton}
import models.AppProtocol.{CreateDepartment, DeleteDepartment, Department, GetDepartmentsReport, UpdateDepartment}
import models.PatientProtocol.{CreateIcds, Icd}
import models.SimpleAuth
import models.UserProtocol.{GetAllUsers, ModifyUser, User, roles}
import models.utils.FileUtils.{SpreadsheetException, parseSpreadsheet}
import models.utils.StringUtils.createHash
import org.webjars.play.WebJarsUtil
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc.{BaseController, ControllerComponents}
import views.html.settings
import models.utils.StringUtils.cyril2Latin

import scala.concurrent.{ExecutionContext, Future}
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
																	 @Named("patient-manager") val patientManager: ActorRef,
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

	def uploadIcds = Action.async(parse.multipartFormData) { implicit request => asyncAuth {
		//    val dataPart = request.body.asFormUrlEncoded
		request.body.file("file").map { filePart =>
			parseSpreadsheet(filePart.ref.path.toFile) match {
				case Right(rows) => createIcds(rows)
				case Left(failReason) => Future.successful(BadRequest(failReason))
			}
		}.getOrElse {
			Future { Ok("File topilmadi.") }
		}
	}}

	private def createIcds(rows: List[List[String]]) = {
		try {
			val icds = validateIcds(rows)
			(patientManager ? CreateIcds(icds)).mapTo[List[Int]].map { ids =>
				logger.info(s"All ICDs imported from file. Count: $ids")
				Ok("OK")
			}.recover { case error =>
				logger.error(s"Creating patients error", error)
				InternalServerError
			}
		} catch {
			case error: SpreadsheetException =>
				logger.warn(s"SpreadsheetException: ", error)
				Future { BadRequest(error.errorText) }
			case error: Throwable =>
				logger.error(s"Validate Patients from spreadsheet Error: ", error)
				Future { InternalServerError }
		}
	}

	private def validateIcds(rows: List[List[String]]) = {
		rows.zipWithIndex.map { case (cyrillicRow, i) =>
			val index = i + 1
			val cells = cyrillicRow.map(cyril2Latin)
			logger.info(s"ROW $index | $cells")

			val throwError = (ind: Int) => throw SpreadsheetException(s"Xatolik aniqlandi. Qator raqami: $index")
			val getRequired = (ind: Int) =>
				if (cells(ind).isEmpty) {
					throwError(ind)
				} else {
					cells(ind)
				}

			val getOptional = (value: String) =>
				if (value.isEmpty) {
					None
				} else {
					Some(value)
				}

			Icd(
				code = getRequired(0),
				name = getOptional(cells(1))
			)
		}
	}

}