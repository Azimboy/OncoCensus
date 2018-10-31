package models.daos

import java.util.Date

import com.google.inject.ImplementedBy
import com.typesafe.scalalogging.LazyLogging
import javax.inject.{Inject, Singleton}
import models.AppProtocol.Department
import models.utils.Date2SqlDate
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.Future

trait DepartmentsComponent
  extends DistrictsComponent { self: HasDatabaseConfigProvider[JdbcProfile] =>
  import dbConfig.profile.api._

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

@ImplementedBy(classOf[DepartmentsDaoImpl])
trait DepartmentsDao {
	def create(department: Department): Future[Int]
	def update(department: Department): Future[Int]
	def delete(id: Int): Future[Int]
	def findById(id: Int): Future[Option[Department]]
	def findAll(): Future[Seq[Department]]
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