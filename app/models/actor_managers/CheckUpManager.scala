package models.actor_managers

import javax.inject.{Inject, Named, Singleton}

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import models.CheckUpProtocol.{AddCheckUp, CheckUp, GetCheckUpsByPatientId}
import models.UserProtocol.{AddUser, GetAllUsers, User}
import models.actor_managers.EncryptionManager._
import models.daos.CheckUpsDao

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CheckUpManager @Inject()(@Named("encryption-manager") encryptionManager: ActorRef,
                                    val checkUpsDao: CheckUpsDao)
                                   (implicit val ec: ExecutionContext)
	extends Actor
		with ActorLogging {

	implicit val defaultTimeout = Timeout(60.seconds)


	override def receive: Receive = {
		case AddCheckUp(checkUp, filePaths) =>
			addCheckUp(checkUp, filePaths).pipeTo(sender())

		case GetCheckUpsByPatientId(patientId) =>
			getCheckUpsByPatientId(patientId).pipeTo(sender())
	}

	def getCheckUpsByPatientId(patientId: Int): Future[Seq[CheckUp]] = {
		for {
			encrCheckUps <- checkUpsDao.findByPatientId(patientId).mapTo[Seq[CheckUp]]
			decrCheckUps <- (encryptionManager ? DecryptCheckUps(encrCheckUps)).mapTo[Seq[CheckUp]]
		} yield decrCheckUps
	}

	def addCheckUp(checkUp: CheckUp, filePaths: Seq[String]): Future[Int] = {
		for {
//			TODO save files
			encrCheckUp <- (encryptionManager ? EncryptCheckUp(checkUp)).mapTo[CheckUp]
			_ = log.info(s"Files Paths = $filePaths")
			id <- checkUpsDao.create(encrCheckUp)
		} yield id
	}

//	def getAllCheckUps(): Future[Seq[CheckUp]] = {
//		for {
//			encrUsers <- checkUpsDao.findAl
//			decrUsers <- (encryptionManager ? DecryptUsers(encrUsers)).mapTo[Seq[User]]
//		} yield decrUsers
//	}

}
