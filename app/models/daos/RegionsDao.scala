package models.daos

import com.typesafe.scalalogging.LazyLogging
import models.AppProtocol.Region
import models.utils.db.DatabaseConnector

import scala.concurrent.{ExecutionContext, Future}

trait RegionsComponent {
	import models.utils.PostgresDriver.api._

	class Regions(tag: Tag) extends Table[Region](tag, "regions") {
		def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
		def name = column[String]("name")

		def * = (id.?, name) <>
			(Region.tupled, Region.unapply _)
	}
}

sealed trait RegionsDao {
	def findById(id: Int): Future[Option[Region]]
	def findAll(): Future[Seq[Region]]
}

class RegionsDaoImpl(val databaseConnector: DatabaseConnector)
										(implicit executionContext: ExecutionContext)
	extends RegionsComponent
		with RegionsDao
		with LazyLogging {

	import databaseConnector._
	import databaseConnector.profile.api._

	val regions = TableQuery[Regions]

	override def findById(id: Int) = {
		db.run {
			regions.filter(_.id === id).result.headOption
		}
	}

	override def findAll(): Future[Seq[Region]] = {
		db.run(regions.result)
	}
}