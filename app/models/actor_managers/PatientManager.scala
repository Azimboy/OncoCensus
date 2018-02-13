package models.actor_managers

import java.nio.file.{Files, Path, Paths}
import javax.inject.{Inject, Named}

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import models.PatientProtocol.{AddPatient, ClientGroup, GetAllClientGroups, GetAllPatients, Patient}
import models.actor_managers.EncryptionManager.{DecryptPatients, EncryptPatient}
import models.daos.{ClientGroupsDao, PatientsDao}
import models.utils.FileUtils
import play.api.{Configuration, Environment}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

class PatientManager  @Inject()(@Named("encryption-manager") encryptionManager: ActorRef,
                                val environment: Environment,
                                val configuration: Configuration,
                                val clientGroupsDao: ClientGroupsDao,
                                val patientsDao: PatientsDao)
                                (implicit val ec: ExecutionContext)
	extends Actor
		with ActorLogging {

	implicit val defaultTimeout = Timeout(60.seconds)

	val appConfig = configuration.get[Configuration]("app")
	val patientAvatarsFolder = appConfig.get[String]("patient-data.avatars-path")
	val patientAvatarsPath = Paths.get(patientAvatarsFolder)

	Files.createDirectories(patientAvatarsPath)

	override def receive: Receive = {
		case GetAllPatients =>
			getAllPatients().pipeTo(sender())

		case AddPatient(patient, photosPath) =>
			addPatient(patient, photosPath).pipeTo(sender())

		case GetAllClientGroups =>
			getAllClientGroups().pipeTo(sender())
	}

	def getAllPatients(): Future[Seq[Patient]] = {
		for {
			encrPatients <- patientsDao.findAll
			decrPatients <- (encryptionManager ? DecryptPatients(encrPatients)).mapTo[Seq[Patient]]
		} yield decrPatients
	}

	def addPatient(patient: Patient, photosPath: Option[Path]): Future[Int] = {
		for {
			encrPatient <- (encryptionManager ? EncryptPatient(patient)).mapTo[Patient]
			_ <- saveIfPhotoExists(photosPath).map { _ =>
				log.info(s"Patients photo successfully saved.")
			}
			dbAction <- patientsDao.create(encrPatient)
		}	yield dbAction
	}

	def getAllClientGroups(): Future[Seq[ClientGroup]] = {
		clientGroupsDao.getAllClientGroups()
	}

	def saveIfPhotoExists(photosPath: Option[Path]): Future[Unit] = {
		photosPath match {
			case Some(photoPath) =>
				val avatarId = Random.alphanumeric.take(10).mkString
				Future {
					FileUtils.saveFile(photoPath, patientAvatarsPath, avatarId)
				}
			case None =>
				Future.successful(())
		}
	}

}
