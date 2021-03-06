package models.actor_managers

import java.nio.file.{Files, Path, Paths}
import java.util.Date
import javax.inject.{Inject, Named}

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import com.typesafe.scalalogging.LazyLogging
import models.AppProtocol.Paging.{PageReq, PageRes}
import models.AppProtocol.{GetDetailedReport, ReportData}
import models.PatientProtocol._
import models.actor_managers.EncryptionManager.{DecryptPatient, DecryptPatients, EncryptPatient}
import models.daos.{IcdsDao, PatientsDao}
import models.utils.FileUtils
import play.api.libs.json.Json
import play.api.{Configuration, Environment}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

class PatientManager  @Inject()(@Named("encryption-manager") encryptionManager: ActorRef,
                                val environment: Environment,
                                val configuration: Configuration,
                                val icdsDao: IcdsDao,
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
		case GetAllPatients(patientsFilter, pageReq) =>
			getAllPatients(patientsFilter, pageReq).pipeTo(sender())

		case GetAllIcds =>
			getAllIcds().pipeTo(sender())

		case ModifyPatient(patient, photosPath) =>
			modifyPatient(patient, photosPath).pipeTo(sender())

		case DeletePatientById(patientId) =>
			deletePatientById(patientId).pipeTo(sender())

		case PatientSupervisedOut(patientId, supervisedOut) =>
			patientSupervisedOut(patientId, supervisedOut).pipeTo(sender())

		case GetDetailedReport(reportData, pageReq) =>
			getPatientsDetailedReport(reportData, pageReq).pipeTo(sender())

		case CreatePatients(patients) =>
			createPatients(patients).pipeTo(sender())

		case CreateIcds(icds) =>
			createIcds(icds).pipeTo(sender())
	}

	def getAllPatients(patientsFilter: PatientsFilter, pageReq: PageReq): Future[PageRes[Patient]] = {
		for {
			encrPatients <- patientsDao.findByFilter(patientsFilter)
			decrPatients <- (encryptionManager ? DecryptPatients(encrPatients)).mapTo[Seq[Patient]]
			byLastName = patientsFilter.lastName match {
				case Some(lastName) => decrPatients.filter(_.lastName.exists(_.toLowerCase.contains(lastName.toLowerCase)))
				case None => decrPatients
			}
			byProvince = patientsFilter.passportId match {
				case Some(province) => byLastName.filter(_.patientDataJson.exists { js =>
					val passNum = (js \ "province").as[String].toLowerCase
					passNum.contains(province.toLowerCase)
				})
				case None => byLastName
			}
			pageRes = pageReq.toPageRes(byProvince)
		} yield pageRes
	}

	def getAllIcds(): Future[Seq[Icd]] = {
		icdsDao.getAllIcds()
	}

	def modifyPatient(patient: Patient, photosPath: Option[Path] = None): Future[Int] = {
		for {
			encrPatient <- (encryptionManager ? EncryptPatient(patient)).mapTo[Patient]
			encrPatientWithAvatar <- saveAvatarIfExists(photosPath, encrPatient)
			dbAction <- (patient.id match {
				case Some(patientId) =>
					log.info(s"EDITING = $patientId")
					patientsDao.findById(patientId).mapTo[Option[Patient]].map { patient =>
						patientsDao.update(encrPatientWithAvatar.copy(createdAt = patient.get.createdAt))
					}
				case None =>
					log.info("NEW PATIENT")
					Future.successful(patientsDao.create(encrPatientWithAvatar.copy(createdAt = Some(new Date))))
			}).flatten
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

	def patientSupervisedOut(patientId: Int, supervisedOut: SupervisedOut): Future[Int] = {
		val supervisedOutJs = Json.toJson(supervisedOut)
		for {
			encrPatient <- patientsDao.findById(patientId).mapTo[Option[Patient]]
			decrPatient <- (encryptionManager ? DecryptPatient(encrPatient.get)).mapTo[Patient]
			editedPatient = decrPatient.copy(
				supervisedOutJson = Some(supervisedOutJs)
			)
			encrPatient <- (encryptionManager ? EncryptPatient(editedPatient)).mapTo[Patient]
			dbAction <- patientsDao.update(encrPatient)
		} yield dbAction
	}

	def getPatientsDetailedReport(reportData: ReportData, pageReq: PageReq): Future[Unit] = {
		Future.successful(())
	}

	def createPatients(patients: List[Patient]) = {
		Future.sequence(patients.map(p => modifyPatient(p)))
	}

	def createIcds(icds: List[Icd]) = {
		Future.sequence(icds.map(icd => icdsDao.create(icd)))
	}

}
