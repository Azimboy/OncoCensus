package models

import akka.actor.ActorSystem
import com.google.inject.ImplementedBy
import com.typesafe.scalalogging.LazyLogging
import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.mvc.Results._
import play.api.mvc._

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.runtime.universe._

@ImplementedBy(classOf[SimpleAuthImpl])
trait SimpleAuth extends LazyLogging {

  implicit val actorSystem: ActorSystem
  implicit private val ex = actorSystem.dispatcher
  private def expiresAtSessionAttrName(sessionAttr: String) = s"$sessionAttr.exp"

  object ErrorText {
    val SessionExpired = "Session expired. Please log in."
    val Unauthorized = "Unauthorized. Please log in."
  }

  def authInit(sessionAttrName: String,
               sessionAttrVal: String,
               sessionDuration: Option[FiniteDuration] = None): Seq[(String, String)] =
  {
    val expiresAtSessionAttr = expiresAtSessionAttrName(sessionAttrName)
    sessionDuration.foldLeft(Map(sessionAttrName -> sessionAttrVal)) { (acc, sessionDur) =>
      val nextExpiration = System.currentTimeMillis() + sessionDur.toMillis
      acc + (expiresAtSessionAttr -> nextExpiration.toString)
    }.toSeq
  }

  def authClear(sessionAttrNames: String*)
               (implicit request: RequestHeader): Session =
  {
    sessionAttrNames.foldLeft(request.session) { (session, name) =>
      session - name - expiresAtSessionAttrName(name)
    }
  }

  def authBy(sessionAttr: String, sessionDuration: Option[FiniteDuration] = None)
            (result: => Result)
            (implicit request: RequestHeader): Result =
  {
    checkAuth(sessionAttr, sessionDuration)(result)
  }

  def authByAsync(sessionAttr: String, sessionDuration: Option[FiniteDuration] = None)
                 (result: => Future[Result])
                 (implicit request: RequestHeader, ec: ExecutionContext): Future[Result] =
  {
    checkAuth(sessionAttr, sessionDuration)(result)
  }

  private def checkAuth[A: TypeTag](sessionAttr: String, sessionDuration: Option[FiniteDuration] = None)(body: => A)(implicit request: RequestHeader): A =
  {
    val expiresAtSessionAttr = expiresAtSessionAttrName(sessionAttr)
    val session = request.session

    def addSessionIfNecessary(result: Result): Result = {
      sessionDuration.map { sessionDur =>
        val currentTime = System.currentTimeMillis()
        val nextExpiration = currentTime + sessionDur.toMillis
        result.addingToSession(expiresAtSessionAttr -> nextExpiration.toString)
      }.getOrElse(result)
    }

    val errorResultOpt: Option[Result] =
      if (session.get(sessionAttr).isEmpty || (sessionDuration.isDefined && session.get(expiresAtSessionAttr).isEmpty)) {
        Some(Unauthorized(ErrorText.Unauthorized))
      } else if (sessionDuration.isDefined && session.get(expiresAtSessionAttr).exists(_.toLong < System.currentTimeMillis())) {
        Some(Unauthorized(ErrorText.SessionExpired))
      } else { None }

    typeOf[A] match {
      case t if t =:= typeOf[Result] =>
        errorResultOpt.getOrElse(
          addSessionIfNecessary(body.asInstanceOf[Result])
        ).asInstanceOf[A]
      case t if t <:< typeOf[Future[Result]] =>
        errorResultOpt.map(Future.successful).getOrElse(
          body.asInstanceOf[Future[Result]].map(addSessionIfNecessary)
        ).asInstanceOf[A]
    }
  }

//  // Return only roles which belong to app/role which user wants to login
//  def getRoleCodesByAppsOrRoles(user: UserAccountBase, loginApps: Seq[CieloApp], loginRoles: Seq[CieloRole]): String = {
//    val userRolesByManagedApps = getRolesByAppCodes(user.managedAppCodes.map(_.split(",").toSeq).getOrElse(Nil))
//    val userRolesByRoleCodes = getRolesByRoleCodes(user.roleCodes.map(_.split(",").toSeq).getOrElse(Nil))
//    val userRoles = userRolesByManagedApps ++ userRolesByRoleCodes
//
//    val wantedRolesByLoginApps = getRolesByAppCodes(loginApps.map(_.code))
//    val wantedRolesByLoginRoles = getRolesByRoleCodes(loginRoles.map(_.code))
//    val wantedRoles = wantedRolesByLoginApps ++ wantedRolesByLoginRoles
//
//    wantedRoles
//        .filter { role =>
//          userRoles.exists(_.code == role.code)
//        }
//        .map(_.code)
//        .distinct
//        .mkString(",")
//  }

}

@Singleton
class SimpleAuthImpl @Inject()(val configuration: Configuration, implicit val actorSystem: ActorSystem)
                              (implicit ec: ExecutionContext)
  extends SimpleAuth {}