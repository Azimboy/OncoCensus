package models.actor_managers

import java.util.Date
import javax.inject.{Inject, Named, Singleton}

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import models.AppProtocol.Paging.{PageReq, PageRes}
import models.AppProtocol.ReportData
import models.CheckUpProtocol.{CheckUp, GetCheckUpsByPatientId, ModifyCheckUp}
import models.StatisticsProtocol.GetDetailedReport
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
		case ModifyCheckUp(checkUp, filePaths) =>
			modifyCheckUp(checkUp, filePaths).pipeTo(sender())

		case GetCheckUpsByPatientId(patientId) =>
			getCheckUpsByPatientId(patientId).pipeTo(sender())

		case GetDetailedReport(reportData, pageReq) =>
			getCheckUpDetailedReport(reportData, pageReq).pipeTo(sender())
	}

	def getCheckUpsByPatientId(patientId: Int): Future[Seq[CheckUp]] = {
		for {
			encrCheckUps <- checkUpsDao.findByPatientId(patientId).mapTo[Seq[CheckUp]]
			decrCheckUps <- (encryptionManager ? DecryptCheckUps(encrCheckUps)).mapTo[Seq[CheckUp]]
		} yield decrCheckUps
	}

	def modifyCheckUp(checkUp: CheckUp, filePaths: Seq[String]): Future[Int] = {
		for {
//			TODO save files
			encrCheckUp <- (encryptionManager ? EncryptCheckUp(checkUp)).mapTo[CheckUp]
			_ = log.info(s"Files Paths = $filePaths")
			dbAction <- (checkUp.id match {
				case Some(checkUpId) =>
					log.info(s"EDITING = $checkUpId")
					checkUpsDao.findById(checkUpId).mapTo[Option[CheckUp]].map { checkUpInDb =>
						checkUpsDao.update(encrCheckUp.copy(createdAt = checkUpInDb.get.createdAt))
					}
				case None =>
					log.info("NEW CHECK UP")
					Future.successful(checkUpsDao.create(encrCheckUp.copy(createdAt = Some(new Date))))
			}).flatten
		} yield dbAction
	}

	def getCheckUpDetailedReport(reportData: ReportData, pageReq: PageReq): Future[PageRes[CheckUp]] = {
		for {
			encrReportData <- (encryptionManager ? EncryptReportData(reportData)).mapTo[ReportData]
			encrCheckUps <- checkUpsDao.getAllCheckUps(encrReportData)
			decrCheckUps <- (encryptionManager ? DecryptCheckUps(encrCheckUps)).mapTo[Seq[CheckUp]]
			pageRes = pageReq.toPageRes(decrCheckUps)
		} yield pageRes
	}

}
