package controllers

import javax.inject._
import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.scalalogging.LazyLogging
import models.AppProtocol.Paging.{PageReq, PageRes}
import models.PatientProtocol.{GetAllPatients, Patient, PatientsFilter}
import models.SimpleAuth
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
                                  implicit val actorSystem: ActorSystem,
                                  implicit val webJarsUtil: WebJarsUtil)
                                 (implicit val ec: ExecutionContext)
  extends BaseController
    with SimpleAuth
    with LazyLogging {

  implicit val defaultTimeout = Timeout(60.seconds)

  def patientIndex = Action { implicit request => auth {
    Ok(reports.patient())
  }}

  def checkUpIndex = Action { implicit request => auth {
    Ok(reports.checkUp())
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

}