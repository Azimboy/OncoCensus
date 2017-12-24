package models.actor_managers

import javax.inject.{Inject, Named}

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import models.PatientProtocol.{AddPatient, ClientGroup, GetAllClientGroups, GetAllPatients, Patient}
import models.actor_managers.EncryptionManager.{DecryptPatients, EncryptPatient}
import models.daos.{ClientGroupsDao, PatientsDao}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}

class PatientManager  @Inject()(@Named("encryption-manager") encryptionManager: ActorRef,
                                val clientGroupsDao: ClientGroupsDao,
                                val patientsDao: PatientsDao)
                                (implicit val ec: ExecutionContext)
	extends Actor
		with ActorLogging {

	implicit val defaultTimeout = Timeout(60.seconds)

	override def receive: Receive = {
		case GetAllPatients =>
			getAllPatients().pipeTo(sender())
		case AddPatient(patient) =>
			addPatient(patient).pipeTo(sender())
		case GetAllClientGroups =>
			getAllClientGroups().pipeTo(sender())
	}

	def getAllPatients(): Future[Seq[Patient]] = {
		for {
			encrPatients <- patientsDao.findAll
			decrPatients <- (encryptionManager ? DecryptPatients(encrPatients)).mapTo[Seq[Patient]]
		} yield decrPatients
	}

	def addPatient(patient: Patient): Future[Int] = {
		for {
			encrPatient <- (encryptionManager ? EncryptPatient(patient)).mapTo[Patient]
			ss <- patientsDao.create(encrPatient)
		}	yield ss
	}

	def getAllClientGroups(): Future[Seq[ClientGroup]] = {
		clientGroupsDao.getAllClientGroups()
	}

}
