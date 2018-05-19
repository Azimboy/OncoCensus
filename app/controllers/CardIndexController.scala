package controllers

import java.io.File
import java.nio.file.Paths
import java.text.SimpleDateFormat

import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.scalalogging.LazyLogging
import javax.inject._
import models.AppProtocol.Paging.{PageReq, PageRes}
import models.CheckUpProtocol.{CheckUp, GetCheckUpsByPatientId, ModifyCheckUp, ReceiveInfo, ReceiveReason, ReceiveType}
import models.PatientProtocol._
import models.SimpleAuth
import org.webjars.play.WebJarsUtil
import play.api.Configuration
import play.api.libs.Files.TemporaryFile
import play.api.libs.json.Json
import play.api.mvc._
import views.html.card_index

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.DurationInt

@Singleton
class CardIndexController @Inject()(val controllerComponents: ControllerComponents,
                                    val configuration: Configuration,
                                    @Named("patient-manager") val patientManager: ActorRef,
                                    @Named("check-up-manager") val checkUpManager: ActorRef,
                                    implicit val actorSystem: ActorSystem,
                                    implicit val webJarsUtil: WebJarsUtil)
                                   (implicit val ec: ExecutionContext)
  extends BaseController
    with SimpleAuth
    with LazyLogging {

  implicit val defaultTimeout = Timeout(60.seconds)

  implicit def getValue(key: String)(implicit request: Request[MultipartFormData[TemporaryFile]]): Option[String] = {
    val value = request.body.dataParts.get(key).flatMap(_.headOption)

    if (value.forall(_.isEmpty)) {
      None
    } else {
      value
    }
  }

  def index = Action { implicit request => auth {
    Ok(card_index.index())
  }}

  def getPatients(page: Int, pageSize: Int) = Action.async(parse.json[PatientsFilter]) { implicit request => asyncAuth {
	  val pageReq = PageReq(page = page, size = pageSize)
    val patientsFilter = request.body
    (patientManager ? GetAllPatients(patientsFilter, pageReq)).mapTo[PageRes[Patient]].map { pageRes =>
      Ok(Json.toJson(pageRes))
    }.recover { case error =>
      logger.error("Error occurred during getting patients", error)
      InternalServerError
    }
  }}

  def modifyPatient = Action.async(parse.multipartFormData) { implicit request => asyncAuth {
	  val patientId = getValue("patientId").map(_.toInt)

	  val patientDataJs = Json.toJson(PatientData(
		  province = "province",
		  street = "street",
		  home = "home",
		  work = "work",
		  position = "position",
		  bloodType = getValue("bloodType").map(BloodType.withName),
      email = "email",
      phoneNumber = "phoneNumber"
	  ))

	  val patient = Patient(
		  id = patientId,
		  firstName = "firstName",
		  lastName = "lastName",
		  middleName = "middleName",
      passportId = getValue("passportId").get,
      gender = getValue("gender").map(Gender.withShortName).get,
		  birthDate = parseDate(getValue("birthDate").get, "dd.MM.yyyy"),
		  villageId = getValue("villageId").map(_.toInt).get,
		  icdId = getValue("icdId").map(_.toInt).get,
		  clientGroup = getValue("clientGroup").map(ClientGroup.withShortName).get,
		  patientDataJson = Some(patientDataJs)
	  )

	  val photosPath = request.body.file("patientsPhoto").map(file => Paths.get(file.ref.getAbsolutePath))

    (patientManager ? ModifyPatient(patient, photosPath)).mapTo[Int].map { _ =>
      Ok("OK")
    }.recover { case error =>
      logger.error("Error occurred during creating new patient", error)
      InternalServerError
    }
  }}

  def deletePatient(patientId: Int) = Action.async { implicit request => asyncAuth {
    (patientManager ? DeletePatientById(patientId)).mapTo[Int].map { _ =>
      Ok(Json.toJson("OK"))
    }.recover { case error =>
      logger.error(s"Error occurred during deleting patient. PatientId: $patientId", error)
      InternalServerError
    }
  }}

  def modifyCheckUp = Action.async(parse.multipartFormData) { implicit request => asyncAuth {
	  val receivedInfoJs = Json.toJson(ReceiveInfo(
		  receiveType = getValue("receiveType").map(ReceiveType.withShortName).getOrElse(ReceiveType.Polyclinic),
		  receiveReason = getValue("receiveReason").map(ReceiveReason.withShortName).getOrElse(ReceiveReason.Simple)
	  ))

    val checkUp = CheckUp(
      id = getValue("checkUpId").map(_.toInt),
      patientId = getValue("patientId").map(_.toInt),
      userId = Some(1),
      startedAt = getValue("startedAt").map(d => parseDate(d)),
      finishedAt = getValue("finishedAt").map(d => parseDate(d)),
      complaint = "complaint",
      objInfo = "objInfo",
      objReview = "objReview",
      statusLocalis = "statusLocalis",
      diagnose = "diagnose",
      recommendation = "recommendation",
	    receiveInfoJson = Some(receivedInfoJs)
    )

    val filePaths = request.body.files.map(_.ref.getAbsolutePath)

    (checkUpManager ? ModifyCheckUp(checkUp, filePaths)).mapTo[Int].map { _ =>
      Ok("OK")
    }.recover { case error =>
      logger.error(s"Error occurred during adding checkUp.", error)
      InternalServerError
    }
  }}

  def getCheckUpsByPatientId(patientId: Int) = Action.async { implicit request => asyncAuth {
    (checkUpManager ? GetCheckUpsByPatientId(patientId)).mapTo[Seq[CheckUp]].map { checkUps =>
      Ok(Json.toJson(checkUps))
    }.recover { case error =>
      logger.error(s"Error occurred during getting check ups. PatientId: $patientId", error)
      InternalServerError
    }
  }}

	def supervisedOut(patientId: Int) = Action.async(parse.json[SupervisedOut]) { implicit request => asyncAuth {
		(patientManager ? PatientSupervisedOut(patientId, request.body)).mapTo[Int].map { _ =>
			Ok("OK")
		}.recover { case error =>
			logger.error(s"Error occurred during changing supervise status.", error)
			InternalServerError
		}
	}}

  def uploadPatients = Action.async (parse.multipartFormData) { implicit request => asyncAuth {
//    val dataPart = request.body.asFormUrlEncoded
    request.body.file("file").map { filePart =>
      val fileName = filePart.filename
      logger.info(s"File = $fileName")
      Future.successful(Ok("OK"))
    }.getOrElse {
      Future { Ok("Missing file") }
    }
  }}

  private def parseDate(dateStr: String, format: String = "dd.MM.yyyy HH:mm") = {
    val dateFormat = new SimpleDateFormat(format)
    dateFormat.parse(dateStr)
  }

}