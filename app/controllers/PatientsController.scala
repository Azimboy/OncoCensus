package controllers

import javax.inject._

import org.webjars.play.WebJarsUtil
import play.api.Configuration
import play.api.mvc._
import views.html.patients

@Singleton
class PatientsController @Inject()(val controllerComponents: ControllerComponents,
                                   val configuration: Configuration,
                                   implicit val webJarsUtil: WebJarsUtil)
  extends BaseController {

  def index = Action {
    Ok(patients.index())
  }

}