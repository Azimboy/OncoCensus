package models.actor_managers

import java.nio.file.{Files, Path, Paths}
import javax.inject.{Inject, Named}

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import com.typesafe.scalalogging.LazyLogging
import models.PatientProtocol.{ClientGroup, DeletePatientById, GetAllClientGroups, GetAllPatients, ModifyPatient, Patient}
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
		with ActorLogging with LazyLogging {

	implicit val defaultTimeout = Timeout(60.seconds)

	val appConfig = configuration.get[Configuration]("app")
	val patientAvatarsFolder = appConfig.get[String]("patient-data.avatars-path")
	val patientAvatarsPath = Paths.get(patientAvatarsFolder)

	Files.createDirectories(patientAvatarsPath)

	override def receive: Receive = {
		case GetAllPatients =>
			getAllPatients().pipeTo(sender())

		case GetAllClientGroups =>
			getAllClientGroups().pipeTo(sender())

		case ModifyPatient(patient, photosPath, isNewPatient) =>
			modifyPatient(patient, photosPath, isNewPatient).pipeTo(sender())

		case DeletePatientById(patientId) =>
			deletePatientById(patientId).pipeTo(sender())
	}
	def getAllPatients(): Future[Seq[Patient]] = {
		for {
			encrPatients <- patientsDao.findAll
			decrPatients <- (encryptionManager ? DecryptPatients(encrPatients)).mapTo[Seq[Patient]]
		} yield decrPatients
	}

	def getAllClientGroups(): Future[Seq[ClientGroup]] = {
		clientGroupsDao.getAllClientGroups()
	}

	def modifyPatient(patient: Patient, photosPath: Option[Path], isNewPatient: Boolean): Future[Int] = {
		for {
			encrPatient <- (encryptionManager ? EncryptPatient(patient)).mapTo[Patient]
			encrPatientWithAvatar <- saveAvatarIfExists(photosPath, encrPatient)
			dbAction <- patient.id match {
				case Some(patientId) =>
					log.info(s"EDITING = $patientId")
					patientsDao.update(encrPatientWithAvatar)
				case None =>
					log.info("NEW PATIENT")
					patientsDao.create(encrPatientWithAvatar)
			}
		}	yield dbAction
	}

	def saveAvatarIfExists(photosPath: Option[Path], patient: Patient): Future[Patient] = {
		photosPath match {
			case Some(photoPath) =>
				val avatarId = Random.alphanumeric.take(10).mkString
				Future {
					FileUtils.saveFile(photoPath, patientAvatarsPath, avatarId)
				}.map { _ =>
					log.info(s"Patients photo successfully saved.")
					patient.copy(avatarId = Some(avatarId))
				}
			case None =>
				Future.successful(patient)
		}
	}

	def deletePatientById(patientId: Int): Future[Int] = {
		patientsDao.delete(patientId)
	}

}
