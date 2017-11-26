package models.actor_managers

import javax.inject.{Inject, Named}

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import models.AppProtocol.{CreateDepartment, DeleteDepartment, Department, DepartmentsReport, GetDepartmentsReport, UpdateDepartment}
import models.actor_managers.EncryptionManager.{DecryptText, EncryptDepartment}
import models.daos.DepartmentsDao

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}

class DepartmentManager @Inject()(@Named("encryption-manager") val encryptionManager: ActorRef,
                                  val departmentsDao: DepartmentsDao)
                                 (implicit val ec: ExecutionContext)
	extends Actor
		with ActorLogging {

	implicit val defaultTimeout = Timeout(60.seconds)

	override def receive: Receive = {
		case GetDepartmentsReport =>
			getAllDepartments.pipeTo(sender())

		case CreateDepartment(department) =>
			addDepartment(department).pipeTo(sender())

		case UpdateDepartment(department) =>
			updateDepartment(department).pipeTo(sender())

		case DeleteDepartment(id) =>
			deleteDepartment(id).pipeTo(sender())
	}

	def getAllDepartments: Future[Seq[DepartmentsReport]] = {
		departmentsDao.findAll.flatMap { departmentsReport =>
			Future.sequence( departmentsReport.map { report =>
				(encryptionManager ? DecryptText(report.name)).mapTo[String].map { decrDepName =>
					report.copy(name = decrDepName)
				}
			})
		}
	}

	def addDepartment(department: Department): Future[Int] = {
		for {
			encrDepartment <- (encryptionManager ? EncryptDepartment(department)).mapTo[Department]
			changes <- departmentsDao.create(encrDepartment)
		} yield changes
	}

	def updateDepartment(department: Department): Future[Int] = {
		for {
			encrDepartment <- (encryptionManager ? EncryptDepartment(department)).mapTo[Department]
			changes <- departmentsDao.update(encrDepartment)
		} yield changes
	}

	def deleteDepartment(id: Int): Future[Int] = {
		departmentsDao.delete(id)
	}
}