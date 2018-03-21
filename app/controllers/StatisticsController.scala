package controllers

import javax.inject._

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.scalalogging.LazyLogging
import models.AppProtocol.Paging.{PageReq, PageRes}
import models.AppProtocol.ReportData
import models.CheckUpProtocol.CheckUp
import models.StatisticsProtocol.GetDetailedReport
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
                                     @Named("check-up-manager") val checkUpManager: ActorRef,
                                     val configuration: Configuration,
                                     implicit val webJarsUtil: WebJarsUtil)
                                    (implicit val ec: ExecutionContext)
  extends BaseController
  with LazyLogging {

  implicit val defaultTimeout = Timeout(60.seconds)

  def index = Action {
    Ok(statistics.index())
  }

  def report(page: Int, pageSize: Int) = Action.async(parse.json[ReportData]) { implicit request => {
    val pageReq = PageReq(page = page, size = pageSize)
    (checkUpManager ? GetDetailedReport(request.body, pageReq)).mapTo[PageRes[CheckUp]].map { pageRes =>
      Ok(Json.toJson(pageRes))
    }.recover {
      case error =>
        logger.error(s"Get report error", error)
        InternalServerError
    }
  }}

}