package controllers

import java.nio.file.{Path, Paths}
import java.text.SimpleDateFormat
import javax.inject._

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.scalalogging.LazyLogging
import models.AppProtocol.Paging.{PageReq, PageRes}
import models.CheckUpProtocol.{AddCheckUp, CheckUp}
import models.PatientProtocol._
import org.webjars.play.WebJarsUtil
import play.api.Configuration
import play.api.libs.Files.TemporaryFile
import play.api.libs.json.Json
import play.api.mvc._
import views.html.card_index

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

@Singleton
class CardIndexController @Inject()(val controllerComponents: ControllerComponents,
                                    val configuration: Configuration,
                                    @Named("patient-manager") val patientManager: ActorRef,
                                    @Named("check-up-manager") val checkUpManager: ActorRef,
                                    implicit val webJarsUtil: WebJarsUtil)
                                   (implicit val ec: ExecutionContext)
  extends BaseController with LazyLogging {

  implicit val defaultTimeout = Timeout(60.seconds)

  implicit def getValue(key: String)(implicit request: Request[MultipartFormData[TemporaryFile]]): Option[String] = {
    val value = request.body.dataParts.get(key).flatMap(_.headOption)

    if (value.forall(_.isEmpty)) {
      None
    } else {
      value
    }
  }

  def index = Action {
    Ok(card_index.index())
  }

  def getPatients(page: Int, pageSize: Int) = Action.async(parse.json[PatientsFilter]) { implicit request =>
	  val pageReq = PageReq(page = page, size = pageSize)
    val patientsFilter = request.body
    (patientManager ? GetAllPatients(pageReq, patientsFilter)).mapTo[PageRes[Patient]].map { pageRes =>
      Ok(Json.toJson(pageRes))
    }.recover { case error =>
      logger.error("Error occurred during getting patients", error)
      InternalServerError
    }
  }

  def getClientGroups = Action.async { implicit request =>
    (patientManager ? GetAllClientGroups).mapTo[Seq[ClientGroup]].map { clientGroups =>
      Ok(Json.toJson(clientGroups))
    }.recover { case error =>
      logger.error("Error occurred during getting client groups", error)
      InternalServerError
    }
  }

  def modifyPatient = Action.async(parse.multipartFormData) { implicit request =>
    val (patient, photosPath) = getPatientData
    (patientManager ? ModifyPatient(patient, photosPath)).mapTo[Int].map { _ =>
      Ok("OK")
    }.recover { case error =>
      logger.error("Error occurred during creating new patient", error)
      InternalServerError
    }
  }

  def deletePatient(patientId: Int) = Action.async { implicit request =>
    (patientManager ? DeletePatientById(patientId)).mapTo[Int].map { _ =>
      Ok(Json.toJson("OK"))
    }.recover { case error =>
      logger.error(s"Error occurred during deleting patient. PatientId: $patientId", error)
      InternalServerError
    }
  }

  def addCheckUp = Action.async(parse.multipartFormData) { implicit request =>
    val checkUp = CheckUp(
      patientId = getValue("patientId").map(_.toInt),
      userId = Some(1),
      startedAt = getValue("startedAt").map(parseDate),
      complaint = "complaint",
      objInfo = "objInfo",
      objReview = "objReview",
      statusLocalis = "statusLocalis",
      diagnose = "diagnose",
      recommendation = "recommendation",
    )

    val filePaths = request.body.files.map(_.ref.getAbsolutePath)

    (checkUpManager ? AddCheckUp(checkUp, filePaths)).mapTo[Int].map { _ =>
      Ok("OK")
    }.recover { case error =>
      logger.error(s"Error occurred during adding checkUp.", error)
      InternalServerError
    }
  }

  private def getPatientData(implicit request: Request[MultipartFormData[TemporaryFile]]): (Patient, Option[Path]) = {
    val patientId = getValue("patientId").map(_.toInt)

    val patientDataJs = Json.toJson(PatientData(
      passportNumber = "passportNumber",
      province = "province",
      street = "street",
      home = "home",
      work = "work",
      position = "position",
      bloodGroup = getValue("bloodGroup").map(BloodGroup.withName)
    ))

    val patient = Patient(
      id = patientId,
      firstName = "firstName",
      lastName = "lastName",
      middleName = "middleName",
      gender = getValue("gender").map(Gender.withShortName),
      birthDate = getValue("birthDate").map(parseDate),
      districtId = getValue("districtId").map(_.toInt),
      clientGroupId = getValue("clientGroupId").map(_.toInt),
      email = "email",
      phoneNumber = "phoneNumber",
      patientDataJson = Some(patientDataJs)
    )

    val photosPath = request.body.file("patientsPhoto").map(file => Paths.get(file.ref.getAbsolutePath))
    logger.info(s"Photo PATH = $photosPath")
    (patient, None)
  }

  private def parseDate(dateStr: String) = {
    val dateFormat = new SimpleDateFormat("dd.MM.yyyy")
    dateFormat.parse(dateStr)
  }

}