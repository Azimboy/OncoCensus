package models.daos

import com.typesafe.scalalogging.LazyLogging
import models.AppProtocol.District
import models.utils.db.DatabaseConnector

import scala.concurrent.{ExecutionContext, Future}

trait DistrictsComponent extends RegionsComponent {
  import models.utils.PostgresDriver.api._

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

sealed trait DistrictsDao {
	def findById(id: Int): Future[Option[District]]
	def findAll(): Future[Seq[District]]
}

class DistrictsDaoImpl(val databaseConnector: DatabaseConnector)
                      (implicit executionContext: ExecutionContext)
	extends DistrictsDao
		with DistrictsComponent
		with LazyLogging {

	import databaseConnector._
	import databaseConnector.profile.api._

	val districts = TableQuery[Districts]

	override def findById(id: Int) = {
		db.run {
			districts.filter(_.id === id).result.headOption
		}
	}

	override def findAll(): Future[Seq[District]] = {
		db.run(districts.result)
	}
}