package models.daos

import java.util.Date

import com.typesafe.scalalogging.LazyLogging
import models.AppProtocol.Department
import models.utils.Date2SqlDate
import models.utils.db.DatabaseConnector

import scala.concurrent.{ExecutionContext, Future}

trait DepartmentsComponent extends DistrictsComponent {
	import models.utils.PostgresDriver.api._

	class Departments(tag: Tag) extends Table[Department](tag, "departments") with Date2SqlDate {
		val districts = TableQuery[Districts]

		def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
		def createdAt = column[Date]("created_at")
		def nameEncr = column[String]("name_encr")
		def districtId = column[Int]("district_id")

		def * = (id.?, createdAt.?, nameEncr, districtId).shaped <>
			(t => {
					val fields =
						(t._1, t._2, t._3, t._4, None, None)
					(Department.apply _).tupled(fields)
				},
				(i: Department) =>
					Department.unapply(i).map { t =>
						(t._1, t._2, t._3, t._4)
					}
			)

		def district = foreignKey("departments_fk_district_id", districtId, districts)(_.id)
	}
}

sealed trait DepartmentsDao {
	def create(department: Department): Future[Int]
	def update(department: Department): Future[Int]
	def delete(id: Int): Future[Int]
	def findById(id: Int): Future[Option[Department]]
	def findAll(): Future[Seq[Department]]
	def getDepartmentsByDistrictId(districtId: Int): Future[Seq[Department]]
}

class DepartmentsDaoImpl(val databaseConnector: DatabaseConnector)
												(implicit executionContext: ExecutionContext)
	extends DepartmentsDao
		with DepartmentsComponent
		with LazyLogging {

	import databaseConnector._
	import databaseConnector.profile.api._

	val regions = TableQuery[Regions]
	val districts = TableQuery[Districts]
	val departments = TableQuery[Departments]

	override def create(department: Department) = {
		db.run {
			(departments returning departments.map(_.id)
				into ((r, id) => id)
				) += department
		}
	}

	override def update(department: Department): Future[Int] = {
		db.run {
			departments.filter(_.id === department.id).update(department)
		}
	}

	override def delete(id: Int): Future[Int] = {
		db.run {
			departments.filter(_.id === id).delete
		}
	}

	override def findById(id: Int) = {
		db.run {
			departments.filter(_.id === id).result.headOption
		}
	}

	override def findAll: Future[Seq[Department]] = {
		val joinWithDistrictsQ = departments.join(districts).on(_.districtId === _.id)
		val joinWithRegionsQ = joinWithDistrictsQ.join(regions).on(_._2.regionId === _.id)

		db.run(joinWithRegionsQ.result).map(_.map { case ((department, district), region) =>
			department.copy(
				region = Some(region),
				district = Some(district)
			)
		}.sortBy(_.id))
	}

	override def getDepartmentsByDistrictId(districtId: Int): Future[Seq[Department]] = {
		db.run(departments.filter(_.districtId === districtId).result)
	}
}