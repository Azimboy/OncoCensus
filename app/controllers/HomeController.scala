package controllers

import javax.inject._

import play.api.Configuration
import play.api.mvc._
import org.webjars.play.WebJarsUtil
import views.html.home

@Singleton
class HomeController @Inject()(val controllerComponents: ControllerComponents,
                               val configuration: Configuration,
                               implicit val webJarsUtil: WebJarsUtil)
  extends BaseController {

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
      case _ => Ok("Not implemented yet")
    }
  }
}