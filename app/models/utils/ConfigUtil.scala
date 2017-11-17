package models.utils

import akka.actor.ActorSystem
import com.typesafe.scalalogging.LazyLogging
import play.api.Configuration

object CieloConfigUtil extends LazyLogging {

  def getWebServerConfig(config: Configuration) = {
    config.get[Configuration]("web-server")
  }

  def getActorSelFromConfig(actorPathParam: String)(implicit actorSystem: ActorSystem, config: Configuration) = {
    actorSystem.actorSelection(getParamFromConfig(actorPathParam))
  }

  def getParamFromConfig(paramName: String)(implicit config: Configuration) = {
    config.get[String](paramName)
  }

  def getBoolParamFromConfig(paramName: String)(implicit config: Configuration) = {
    config.get[Boolean](paramName)
  }

  def getConfigFromConfig(paramName: String)(implicit config: Configuration) = {
    config.get[Configuration](paramName)
  }

}
