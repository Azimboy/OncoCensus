package models.actor_managers

import java.util.Date
import javax.inject.{Inject, Named}

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import models.AppProtocol.{CreateDepartment, DeleteDepartment, Department, District, GetAllDistricts, GetAllRegions, GetDepartmentsReport, Region, UpdateDepartment}
import models.actor_managers.EncryptionManager.{DecryptText, EncryptDepartment}
import models.daos.{DepartmentsDao, DistrictsDao, RegionsDao}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}

class DepartmentManager @Inject()(@Named("encryption-manager") val encryptionManager: ActorRef,
                                  val regionsDao: RegionsDao,
                                  val districtsDao: DistrictsDao,
                                  val departmentsDao: DepartmentsDao)
                                 (implicit val ec: ExecutionContext)
	extends Actor
		with ActorLogging {

	implicit val defaultTimeout = Timeout(60.seconds)

	override def receive: Receive = {
		case GetAllRegions =>
			getAllRegions().pipeTo(sender())

		case GetAllDistricts =>
			getDistricts().pipeTo(sender())

		case GetDepartmentsReport =>
			getAllDepartments.pipeTo(sender())

		case CreateDepartment(department) =>
			addDepartment(department).pipeTo(sender())

		case UpdateDepartment(department) =>
			updateDepartment(department).pipeTo(sender())

		case DeleteDepartment(id) =>
			deleteDepartment(id).pipeTo(sender())
	}

	def getAllRegions(): Future[Seq[Region]] = {
		regionsDao.getAllRegions()
	}

	def getDistricts(): Future[Seq[District]] = {
		regionsDao.getAllRegions().flatMap { regions =>
			Future.sequence(
				regions.map { region =>
					districtsDao.getDistrictsByRegionId(region.id.get)
				}
			).map(_.flatten)
		}
	}

	def getAllDepartments(): Future[Seq[Department]] = {
		departmentsDao.findAll.flatMap { departments =>
			Future.sequence( departments.map { report =>
				(encryptionManager ? DecryptText(report.name)).mapTo[String].map { decrDepName =>
					report.copy(name = decrDepName)
				}
			})
		}
	}

	def addDepartment(department: Department): Future[Int] = {
		for {
			encrDepartment <- (encryptionManager ? EncryptDepartment(department.copy(createdAt = Some(new Date)))).mapTo[Department]
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