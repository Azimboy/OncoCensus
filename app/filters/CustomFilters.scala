package filters

import javax.inject.Inject

import play.api.Configuration
import play.api.http.HttpFilters

class CustomFilters @Inject() (configuration: Configuration,
                               secureFlashFilter: SecureFlashFilter,
//                               securityHeadersFilter: SecurityHeadersFilter
                               securityHeadersFilter: CustomSecurityHeadersFilter)
  extends HttpFilters
{
  private val conf = configuration.get[Configuration]("app")
  val useSecureFlashCookie = conf.getOptional[Boolean]("use-secure-flash-cookie").exists(x => x)

  val filters = if (useSecureFlashCookie) {
    Seq(secureFlashFilter, securityHeadersFilter)
  } else {
    Seq(securityHeadersFilter)
  }
}