package filters

import javax.inject.Inject

//import org.jboss.netty.handler.codec.http.HttpHeaders.Names._
import play.api.mvc._

import scala.concurrent.ExecutionContext

class SecureFlashFilter @Inject()(val flash: FlashCookieBaker)
                                 (implicit ex: ExecutionContext) extends EssentialFilter {

  def apply(next: EssentialAction) = EssentialAction { req =>
    next(req).map { result =>
      val resultCookies = result.header.headers.get("Set-Cookie").map(Cookies.decodeSetCookieHeader)

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
          result.withHeaders("Set-Cookie" ->
            Cookies.mergeSetCookieHeader(result.header.headers.getOrElse("Set-Cookie", ""),
              Seq(secureFlashCookie))
          )
        case None => result
      }
    }
  }
}
