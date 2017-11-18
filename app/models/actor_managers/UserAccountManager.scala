package models.actor_managers

import javax.inject.{Inject, Named}

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import models.AppProtocol.{District, Region}
import models.UserAccountProtocol.{AddUserAccount, GetAllRegions, GetAllUserAccounts, GetDistrictsByRegionId, UserAccount}
import models.actor_managers.EncryptionManager.{DecryptUserAccounts, EncryptUserAccount}
import models.daos.{DistrictsDao, RegionsDao, UserAccountsDao}

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

class UserAccountManager @Inject()(@Named("encryption-manager") encryptionManager: ActorRef,
                                   val userAccountsDao: UserAccountsDao,
                                   val regionsDao: RegionsDao,
                                   val districtsDao: DistrictsDao)
	extends Actor
		with ActorLogging {

	implicit val executionContext = context.dispatcher
	implicit val defaultTimeout = Timeout(60.seconds)

	override def receive: Receive = {
		case AddUserAccount(userAccount) =>
			addUserAccount(userAccount).pipeTo(sender())

		case GetAllUserAccounts =>
			getAllUserAccounts().pipeTo(sender())

		case GetAllRegions =>
			getAllRegions().pipeTo(sender())

		case GetDistrictsByRegionId(regionId) =>
			getDistrictsByRegionId(regionId).pipeTo(sender())
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

	def getAllRegions(): Future[Seq[Region]] = {
		regionsDao.getAllRegions()
	}

	def getDistrictsByRegionId(regionId: Int): Future[Seq[District]] = {
		districtsDao.getDistrictsByRegionId(regionId)
	}
}
