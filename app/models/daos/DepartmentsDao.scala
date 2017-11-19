package models.daos

import javax.inject.{Inject, Singleton}

import com.google.inject.ImplementedBy
import com.typesafe.scalalogging.LazyLogging
import models.AppProtocol.Department
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
	def findById(id: Int): Future[Option[Department]]
	def create(department: Department): Future[Int]
	def getDepartmentsByDistrictId(districtId: Int): Future[Seq[Department]]
}

@Singleton
class DepartmentsDaoImpl @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
	extends DepartmentsDao
		with DepartmentsComponent
		with HasDatabaseConfigProvider[JdbcProfile]
		with LazyLogging {

	import dbConfig.profile.api._

	val departments = TableQuery[Departments]

	override def findById(id: Int) = {
		db.run {
			departments.filter(_.id === id).result.headOption
		}
	}

	override def create(department: Department) = {
		db.run {
			(departments returning departments.map(_.id)
				into ((r, id) => id)
				) += department
		}
	}

	override def getDepartmentsByDistrictId(districtId: Int): Future[Seq[Department]] = {
		db.run(departments.filter(_.districtId === districtId).result)
	}
}