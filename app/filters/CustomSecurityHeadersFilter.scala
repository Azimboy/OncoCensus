package filters

import play.api.mvc._

class CustomSecurityHeadersFilter extends EssentialFilter {
  import scala.concurrent.ExecutionContext.Implicits.global

  def apply(next: EssentialAction) = EssentialAction { req =>
    next(req).map { result =>
      if (req.path.startsWith("/TDDI/") || req.path.startsWith("/sms-api/")) { // /sms-api/ is for WEX
        result
      } else {
        result.withHeaders("X-Frame-Options" -> "SAMEORIGIN")
      }
    }
  }
}
