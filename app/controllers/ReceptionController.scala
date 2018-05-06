package controllers

import akka.actor.ActorSystem
import javax.inject._
import models.SimpleAuth
import org.webjars.play.WebJarsUtil
import play.api.Configuration
import play.api.mvc._
import views.html.reception

@Singleton
class ReceptionController @Inject()(val controllerComponents: ControllerComponents,
                                    val configuration: Configuration,
                                    implicit val actorSystem: ActorSystem,
                                    implicit val webJarsUtil: WebJarsUtil)
  extends BaseController
    with SimpleAuth {

  def index = Action { implicit request => auth {
    Ok(reception.index())
  }}

}