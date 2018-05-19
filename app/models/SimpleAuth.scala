package models

import akka.actor.ActorSystem
import com.google.inject.ImplementedBy
import com.typesafe.scalalogging.LazyLogging
import javax.inject.{Inject, Singleton}
import models.UserProtocol._
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
    val SessionExpired = "Sessiya muddati tugadi. Iltimos qaytadan tizimga kiring."
    val Unauthorized = "Tizimga kirilmagan. Iltimos oldin tizimga kiring."
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
               (implicit request: RequestHeader): Session = {
    sessionAttrNames.foldLeft(request.session) { (session, name) =>
      session - name - expiresAtSessionAttrName(name)
    }
  }

  def auth(result: => Result)(implicit request: RequestHeader): Result =
    checkAuth(userSessionKey, sessionDuration)(result)

  def asyncAuth(result: => Future[Result])(implicit request: RequestHeader, ec: ExecutionContext): Future[Result] =
    checkAuth(userSessionKey, sessionDuration)(result)

  private def checkAuth[A: TypeTag](sessionAttr: String, sessionDuration: FiniteDuration)(body: => A)(implicit request: RequestHeader): A = {
    val expiresAtSessionAttr = expiresAtSessionAttrName(sessionAttr)
    val session = request.session

    def addSessionIfNecessary(result: Result): Result = {
      val currentTime = System.currentTimeMillis()
      val nextExpiration = currentTime + sessionDuration.toMillis
      result.addingToSession(expiresAtSessionAttr -> nextExpiration.toString)
    }

    val errorResultOpt: Option[Result] =
      if (session.get(sessionAttr).isEmpty || session.get(expiresAtSessionAttr).isEmpty) {
        Some(Unauthorized(ErrorText.Unauthorized))
      } else if (session.get(expiresAtSessionAttr).exists(_.toLong < System.currentTimeMillis())) {
        Some(Unauthorized(ErrorText.SessionExpired))
      } else {
        None
      }

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

}

@Singleton
class SimpleAuthImpl @Inject()(val configuration: Configuration, implicit val actorSystem: ActorSystem)
                              (implicit ec: ExecutionContext)
  extends SimpleAuth {}