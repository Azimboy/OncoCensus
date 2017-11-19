package models.daos

import javax.inject.{Inject, Singleton}

import com.google.inject.ImplementedBy
import com.typesafe.scalalogging.LazyLogging
import models.AppProtocol.District
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.Future

trait DistrictsComponent extends RegionsComponent
{ self: HasDatabaseConfigProvider[JdbcProfile] =>

	import dbConfig.profile.api._

	class Districts(tag: Tag) extends Table[District](tag, "districts") {
		val regions = TableQuery[Regions]

		def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
		def name = column[String]("name")
		def regionId = column[Int]("region_id")

		def * = (id.?, name, regionId) <>
			(District.tupled, District.unapply _)

		def region = foreignKey("districts_fk_region_id", regionId, regions)(_.id)
	}
}

@ImplementedBy(classOf[DistrictsDaoImpl])
trait DistrictsDao {
	def findById(id: Int): Future[Option[District]]
	def getDistrictsByRegionId(regionId: Int): Future[Seq[District]]
}

@Singleton
class DistrictsDaoImpl @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
	extends DistrictsDao
		with DistrictsComponent
		with HasDatabaseConfigProvider[JdbcProfile]
		with LazyLogging {

	import dbConfig.profile.api._

	val districts = TableQuery[Districts]

	override def findById(id: Int) = {
		db.run {
			districts.filter(_.id === id).result.headOption
		}
	}

	override def getDistrictsByRegionId(regionId: Int): Future[Seq[District]] = {
		db.run(districts.filter(_.regionId === regionId).result)
	}
}