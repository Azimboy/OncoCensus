package controllers

import java.util.Date
import javax.inject._

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.scalalogging.LazyLogging
import models.PatientProtocol.{AddPatient, Gender, GetAllPatients, Patient, PatientData}
import org.webjars.play.WebJarsUtil
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc._
import views.html.card_index

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

object CardIndexController {
  case class PatientWeb(
    firstName: Option[String],
    lastName: Option[String],
    middleName: Option[String],
    gender: String,
    birthDate: Option[Date],
    districtId: Int,
    clientGroupId: Option[Int],
    email: Option[String],
    phoneNumber: Option[String],
    patientDataJson: PatientData
  )

  implicit val patientWebFormat = Json.format[PatientWeb]
}

@Singleton
class CardIndexController @Inject()(val controllerComponents: ControllerComponents,
                                    val configuration: Configuration,
                                    @Named("patient-manager") val patientManager: ActorRef,
                                    implicit val webJarsUtil: WebJarsUtil)
                                   (implicit val ec: ExecutionContext)
  extends BaseController with LazyLogging {

  implicit val defaultTimeout = Timeout(60.seconds)

  import CardIndexController._

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

  def createPatient = Action.async(parse.json[PatientWeb]) { implicit request =>
    val patientWeb = request.body

    val patientDataJs = Json.toJson(patientWeb.patientDataJson)

    val newPatient = Patient(
      createdAt = Some(new Date),
      firstName = patientWeb.firstName,
      lastName = patientWeb.lastName,
      middleName = patientWeb.middleName,
      gender = Some(Gender.withShortName(patientWeb.gender)),
      birthDate = Some(new Date),
      districtId = Some(patientWeb.districtId),
      clientGroupId = patientWeb.clientGroupId,
      email = patientWeb.email,
      phoneNumber = patientWeb.phoneNumber,
      patientDataJson = Some(patientDataJs)
    )

    (patientManager ? AddPatient(newPatient)).mapTo[Int].map { id =>
      Ok(Json.toJson(id))
    }.recover { case error =>
      logger.error("Add user error", error)
      InternalServerError
    }
  }

}