package filters

import javax.inject.Inject

import org.jboss.netty.handler.codec.http.HttpHeaders.Names._
import play.api.mvc._

class SecureFlashFilter @Inject()(val flash: FlashCookieBaker) extends EssentialFilter {
  import scala.concurrent.ExecutionContext.Implicits.global

  def apply(next: EssentialAction) = EssentialAction { req =>
    next(req).map { result =>
      val resultCookies = result.header.headers.get(SET_COOKIE).map(Cookies.decodeSetCookieHeader)

      // Get either a set flash cookie, or if the incoming request had a flash cookie, a discarding cookie
      val flashCookie = resultCookies.flatMap(_.find(_.name == flash.COOKIE_NAME))
        .orElse {
          Option(req.flash).filterNot(_.isEmpty).map { _ =>
            Flash.discard.toCookie
          }
        }

      flashCookie match {
        case Some(cookie) =>
          val secureFlashCookie = cookie.copy(secure = true)
          result.withHeaders(SET_COOKIE ->
            Cookies.mergeSetCookieHeader(result.header.headers.getOrElse(SET_COOKIE, ""),
              Seq(secureFlashCookie))
          )
        case None => result
      }
    }
  }
}
