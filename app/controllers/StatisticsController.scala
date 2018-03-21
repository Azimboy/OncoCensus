package controllers

import javax.inject._

import akka.pattern.ask
import akka.actor.ActorRef
import akka.util.Timeout
import models.AppProtocol.Paging.PageReq
import models.AppProtocol.ReportData
import models.PatientProtocol.GetPatientsDetailedReport
import org.webjars.play.WebJarsUtil
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc._
import views.html.statistics

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

@Singleton
class StatisticsController @Inject()(val controllerComponents: ControllerComponents,
                                     @Named("patient-manager") val patientManager: ActorRef,
                                     val configuration: Configuration,
                                     implicit val webJarsUtil: WebJarsUtil)
                                    (implicit val ec: ExecutionContext)
  extends BaseController {

  implicit val defaultTimeout = Timeout(60.seconds)

  def index = Action {
    Ok(statistics.index())
  }

  def report(page: Int, pageSize: Int) = Action.async(parse.json[ReportData]) { implicit request => {
    val pageReq = PageReq(page = page, size = pageSize)
    (patientManager ? GetPatientsDetailedReport(request.body, pageReq)).map { pageRes =>
//      Ok(Json.toJson(pageRes))
      Ok(Json.toJson(""))
    }
  }}

}