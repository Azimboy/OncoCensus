package controllers

import javax.inject._

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.scalalogging.LazyLogging
import models.AppProtocol.{District, GetAllDistricts, GetAllRegions, Region}
import org.webjars.play.WebJarsUtil
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc._
import views.html.home

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

@Singleton
class HomeController @Inject()(val controllerComponents: ControllerComponents,
                               val configuration: Configuration,
                               @Named("department-manager") val departmentManager: ActorRef,
                               implicit val webJarsUtil: WebJarsUtil)
                              (implicit val ec: ExecutionContext)
  extends BaseController with LazyLogging {

  implicit val defaultTimeout = Timeout(60.seconds)

  def index = Action {
    Ok(home.index())
  }

  def login = Action {
    Ok(home.login())
  }

  def redirectTo(menuName: String) = Action {
    menuName match {
      case "indicator" => Redirect(controllers.routes.IndicatorController.index())
      case "reception" => Redirect(controllers.routes.ReceptionController.index())
      case "cardIndex" => Redirect(controllers.routes.CardIndexController.index())
      case "patientReports" => Redirect(controllers.routes.ReportsController.patientIndex())
      case "checkUpReports" => Redirect(controllers.routes.ReportsController.checkUpIndex())
      case "settings" => Redirect(controllers.routes.SettingsController.index())
      case _ => Ok("Not implemented yet")
    }
  }

  def getRegions = Action.async { implicit request =>
    (departmentManager ? GetAllRegions).mapTo[Seq[Region]].map { regions =>
      Ok(Json.toJson(regions))
    }
  }

  def getDistricts = Action.async { implicit request =>
    (departmentManager ? GetAllDistricts).mapTo[Seq[District]].map { districts =>
      Ok(Json.toJson(districts))
    }
  }

}