package controllers

import javax.inject.Inject

import org.webjars.play.WebJarsUtil
import play.api.Configuration
import play.api.mvc.{BaseController, ControllerComponents}
import views.html.settings

class SettingsController @Inject()(val controllerComponents: ControllerComponents,
                                   val configuration: Configuration,
                                   implicit val webJarsUtil: WebJarsUtil)
	extends BaseController {

	def index = Action {
		Ok(settings.index())
	}

}