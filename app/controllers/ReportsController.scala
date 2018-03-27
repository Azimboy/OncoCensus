package controllers

import javax.inject._

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.scalalogging.LazyLogging
import models.AppProtocol.Paging.{PageReq, PageRes}
import models.PatientProtocol.{GetAllPatients, Patient, PatientsFilter}
import org.webjars.play.WebJarsUtil
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc._
import views.html.reports

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

@Singleton
class ReportsController @Inject()(val controllerComponents: ControllerComponents,
                                  @Named("patient-manager") val patientManager: ActorRef,
                                  @Named("check-up-manager") val checkUpManager: ActorRef,
                                  val configuration: Configuration,
                                  implicit val webJarsUtil: WebJarsUtil)
                                 (implicit val ec: ExecutionContext)
  extends BaseController
  with LazyLogging {

  implicit val defaultTimeout = Timeout(60.seconds)

  def patientIndex = Action {
    Ok(reports.patient())
  }

  def checkUpIndex = Action {
    Ok(reports.checkUp())
  }

  def getPatients(page: Int, pageSize: Int) = Action.async(parse.json[PatientsFilter]) { implicit request =>
    val pageReq = PageReq(page = page, size = pageSize)
    val patientsFilter = request.body
    (patientManager ? GetAllPatients(patientsFilter, pageReq)).mapTo[PageRes[Patient]].map { pageRes =>
      Ok(Json.toJson(pageRes))
    }.recover { case error =>
      logger.error("Error occurred during getting patients", error)
      InternalServerError
    }
  }

}