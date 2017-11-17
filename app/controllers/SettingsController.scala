package controllers

import javax.inject.Inject

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.scalalogging.LazyLogging
import models.UserAccountProtocol.{AddUserAccount, UserAccount}
import models.utils.CieloConfigUtil._
import org.webjars.play.WebJarsUtil
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc.{BaseController, ControllerComponents}
import views.html.settings

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

class SettingsController @Inject()(val controllerComponents: ControllerComponents,
                                   val configuration: Configuration,
                                   implicit val webJarsUtil: WebJarsUtil,
                                   implicit val actorSystem: ActorSystem
																	 )(implicit val ec: ExecutionContext)
	extends BaseController with LazyLogging {

	implicit val defaultTimeout = Timeout(60.seconds)

	implicit val currentConfig = getWebServerConfig(configuration)
	val userAccountManager = getActorSelFromConfig("actor-path.user-account-manager")

	def index = Action {
		Ok(settings.index())
	}

	def getUsers = Action {
		Ok(settings.index())
	}

	def addUserManager() = Action.async(parse.json[UserAccount]) { implicit request =>
		val user = request.body

		(userAccountManager ? AddUserAccount(user)).mapTo[Int].map { id =>
			Ok(Json.toJson(id))
		}.recover { case error =>
			logger.error("Add user error", error)
			InternalServerError
		}
	}

}