package controllers

import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.Date
import javax.inject._

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.scalalogging.LazyLogging
import models.PatientProtocol.{AddPatient, BloodGroup, ClientGroup, Gender, GetAllClientGroups, GetAllPatients, Patient, PatientData}
import org.webjars.play.WebJarsUtil
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc._
import views.html.card_index

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

@Singleton
class CardIndexController @Inject()(val controllerComponents: ControllerComponents,
                                    val configuration: Configuration,
                                    @Named("patient-manager") val patientManager: ActorRef,
                                    implicit val webJarsUtil: WebJarsUtil)
                                   (implicit val ec: ExecutionContext)
  extends BaseController with LazyLogging {

  implicit val defaultTimeout = Timeout(60.seconds)

  def index = Action {
    Ok(card_index.index())
  }

  def getPatients = Action.async { implicit request =>
    (patientManager ? GetAllPatients).mapTo[Seq[Patient]].map { patients =>
      Ok(Json.toJson(patients))
    }.recover { case error =>
      logger.error("Patients", error)
      InternalServerError
    }
  }

  def createPatient = Action.async(parse.multipartFormData) { implicit request =>
    val multipartBody = request.body

    implicit def getValue(key: String): Option[String] = {
      val value = multipartBody.dataParts.get(key).flatMap(_.headOption)
      if (value.forall(_.isEmpty)) {
        None
      } else {
        value
      }
    }

    val patientDataJs = Json.toJson(PatientData(
      passportNo = "passportNo",
      province = "province",
      street = "street",
      home = "home",
      work = "work",
      position = "position",
      bloodGroup = getValue("bloodGroup").map(BloodGroup.withName)
    ))

    val newPatient = Patient(
      createdAt = Some(new Date),
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

    val photosPath = multipartBody.file("patientsPhoto").map(file => Paths.get(file.ref.getAbsolutePath))

    logger.info(s"Photo PATH = $photosPath")
    (patientManager ? AddPatient(newPatient, photosPath)).mapTo[Int].map { id =>
      Ok(Json.toJson(id))
    }.recover { case error =>
      logger.error("Error accurred during creating new patient", error)
      InternalServerError
    }
  }

  def getClientGroups = Action.async { implicit request =>
    (patientManager ? GetAllClientGroups).mapTo[Seq[ClientGroup]].map { clientGroups =>
      Ok(Json.toJson(clientGroups))
    }.recover { case error =>
      logger.error("ClientGroups", error)
      InternalServerError
    }
  }

  private def parseDate(dateStr: String) = {
    val dateFormat = new SimpleDateFormat("dd.MM.yyyy")
    dateFormat.parse(dateStr)
  }

}