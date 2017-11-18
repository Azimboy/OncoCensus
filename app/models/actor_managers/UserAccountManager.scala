package models.actor_managers

import javax.inject.{Inject, Named}

import akka.pattern.{ask, pipe}
import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.util.Timeout
import models.UserAccountProtocol.{AddUserAccount, GetAllUserAccounts, UserAccount}
import models.actor_managers.EncryptionManager.{DecryptUserAccount, DecryptUserAccounts, EncryptUserAccount}
import models.daos.UserAccountsDao

import scala.concurrent.duration.DurationInt
import scala.concurrent.Future

class UserAccountManager @Inject()(@Named("encryption-manager") encryptionManager: ActorRef,
                                   val userAccountsDao: UserAccountsDao)
	extends Actor
		with ActorLogging {

	implicit val executionContext = context.dispatcher
	implicit val defaultTimeout = Timeout(60.seconds)

	override def receive: Receive = {
		case AddUserAccount(userAccount) =>
			addUserAccount(userAccount).pipeTo(sender())

		case GetAllUserAccounts =>
			getAllUserAccounts().pipeTo(sender())
	}

	def addUserAccount(newUserAccount: UserAccount): Future[Int] = {
		for {
			encryptedUserAccount <- (encryptionManager ? EncryptUserAccount(newUserAccount)).mapTo[UserAccount]
			id <- userAccountsDao.create(encryptedUserAccount)
		} yield id
	}

	def getAllUserAccounts(): Future[Seq[UserAccount]] = {
		for {
			encrUserAccounts <- userAccountsDao.findAll
			decrUserAccounts <- (encryptionManager ? DecryptUserAccounts(encrUserAccounts)).mapTo[Seq[UserAccount]]
		} yield decrUserAccounts
	}
}
