package models.daos

import com.google.inject.ImplementedBy
import com.typesafe.scalalogging.LazyLogging
import javax.inject.{Inject, Singleton}
import models.AppProtocol.Region
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.Future

trait RegionsComponent { self: HasDatabaseConfigProvider[JdbcProfile] =>
	import dbConfig.profile.api._

	class Regions(tag: Tag) extends Table[Region](tag, "regions") {
		def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
		def name = column[String]("name")

		def * = (id.?, name) <>
			(Region.tupled, Region.unapply _)
	}
}

@ImplementedBy(classOf[RegionsDaoImpl])
trait RegionsDao {
	def findById(id: Int): Future[Option[Region]]
	def findAll(): Future[Seq[Region]]
}

@Singleton
class RegionsDaoImpl @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
	extends RegionsComponent
		with RegionsDao
		with HasDatabaseConfigProvider[JdbcProfile]
		with LazyLogging {

	import dbConfig.profile.api._

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