package models.actor_managers

import javax.inject.{Inject, Named}

import akka.pattern.{ask, pipe}
import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.util.Timeout
import models.UserAccountsProtocol.{AddUserAccount, UserAccount}
import models.actor_managers.EncryptionManager.EncryptUserAccount
import models.daos.UserAccountsDao

import scala.concurrent.duration.DurationInt
import scala.concurrent.Future

class UserAccountsManager @Inject() (@Named("encryption-manager") encryptionManager: ActorRef,
                                     val userAccountsDao: UserAccountsDao)
	extends Actor
		with ActorLogging {

	implicit val executionContext = context.dispatcher
	implicit val defaultTimeout = Timeout(60.seconds)

	override def receive: Receive = {
		case AddUserAccount(userAccount) =>
			addUserAccount(userAccount).pipeTo(sender())
	}

	private def addUserAccount(newUserAccount: UserAccount): Future[Int] = {
		for {
			encryptedUserAccount <- (encryptionManager ? EncryptUserAccount(newUserAccount)).mapTo[UserAccount]
			id <- userAccountsDao.create(encryptedUserAccount)
		} yield id
	}

}
