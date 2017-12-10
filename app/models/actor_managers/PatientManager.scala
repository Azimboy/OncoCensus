package models.actor_managers

import javax.inject.{Inject, Named}

import akka.pattern.{ask, pipe}
import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.util.Timeout
import models.PatientProtocol.{AddPatient, GetAllPatients, Patient}
import models.actor_managers.EncryptionManager.{DecryptPatients, EncryptPatient}
import models.daos.PatientsDao

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.DurationInt

class PatientManager  @Inject()(@Named("encryption-manager") encryptionManager: ActorRef,
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
	}

	def getAllPatients() = {
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

}
