package models.daos

import javax.inject.{Inject, Singleton}

import com.google.inject.ImplementedBy
import com.typesafe.scalalogging.LazyLogging
import models.AppProtocol.{Department, DepartmentsReport}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.Future

trait DepartmentsComponent extends DistrictsComponent
{ self: HasDatabaseConfigProvider[JdbcProfile] =>

	import dbConfig.profile.api._

	class Departments(tag: Tag) extends Table[Department](tag, "departments") {
		val districts = TableQuery[Districts]

		def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
		def nameEncr = column[String]("name_encr")
		def districtId = column[Int]("district_id")

		def * = (id.?, nameEncr, districtId) <>
			(Department.tupled, Department.unapply _)

		def district = foreignKey("departments_fk_district_id", districtId, districts)(_.id)
	}
}

@ImplementedBy(classOf[DepartmentsDaoImpl])
trait DepartmentsDao {
	def create(department: Department): Future[Int]
	def update(department: Department): Future[Int]
	def delete(id: Int): Future[Int]
	def findById(id: Int): Future[Option[Department]]
	def findAll: Future[Seq[DepartmentsReport]]
	def getDepartmentsByDistrictId(districtId: Int): Future[Seq[Department]]
}

@Singleton
class DepartmentsDaoImpl @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
	extends DepartmentsDao
		with DepartmentsComponent
		with HasDatabaseConfigProvider[JdbcProfile]
		with LazyLogging {

	import dbConfig.profile.api._
	import scala.concurrent.ExecutionContext.Implicits.global

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

	override def findAll: Future[Seq[DepartmentsReport]] = {
		val joinWithDistrictsQ = departments.join(districts).on(_.districtId === _.id)
		val joinWithRegionsQ = joinWithDistrictsQ.join(regions).on(_._2.regionId === _.id)

		db.run(joinWithRegionsQ.result).map(_.map { case ((department, district), region) =>
			DepartmentsReport(
				id = department.id,
				name = department.name,
				regionName = region.name,
				regionId = region.id.get,
				districtName = district.name,
				districtId = district.id.get
			)
		}.sortBy(_.id))
	}

	override def getDepartmentsByDistrictId(districtId: Int): Future[Seq[Department]] = {
		db.run(departments.filter(_.districtId === districtId).result)
	}
}