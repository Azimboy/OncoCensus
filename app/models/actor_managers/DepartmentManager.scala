package models.actor_managers

import java.util.Date

import javax.inject.{Inject, Named}
import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import models.AppProtocol.{CreateDepartment, DeleteDepartment, Department, District, GetAllDistricts, GetAllRegions, GetAllVillages, GetDepartmentsReport, Region, UpdateDepartment, Village}
import models.actor_managers.EncryptionManager.{DecryptDepartments, DecryptText, EncryptDepartment}
import models.daos.{DepartmentsDao, DistrictsDao, RegionsDao, VillagesDao}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}

class DepartmentManager @Inject()(@Named("encryption-manager") val encryptionManager: ActorRef,
                                  val regionsDao: RegionsDao,
                                  val districtsDao: DistrictsDao,
                                  val villagesDao: VillagesDao,
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

		case GetAllVillages =>
			getVillages().pipeTo(sender())

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
		regionsDao.findAll()
	}

	def getDistricts(): Future[Seq[District]] = {
		districtsDao.findAll()
	}

	def getVillages(): Future[Seq[Village]] = {
		villagesDao.findAll()
	}

	def getAllDepartments(): Future[Seq[Department]] = {
		for {
			departments <- departmentsDao.findAll()
			decrDepartments <- (encryptionManager ? DecryptDepartments(departments)).mapTo[Seq[Department]]
		} yield decrDepartments
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