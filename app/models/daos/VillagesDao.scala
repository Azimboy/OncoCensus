package models.daos

import com.google.inject.ImplementedBy
import com.typesafe.scalalogging.LazyLogging
import javax.inject.{Inject, Singleton}
import models.AppProtocol.Village
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.Future

trait VillagesComponent
	extends DistrictsComponent { self: HasDatabaseConfigProvider[JdbcProfile] =>
	import dbConfig.profile.api._

	class Villages(tag: Tag) extends Table[Village](tag, "villages") {
		val districts = TableQuery[Districts]

		def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
		def name = column[String]("name")
		def districtId = column[Int]("district_id")

		def * = (id.?, name, districtId) <>
			(Village.tupled, Village.unapply _)

		def district = foreignKey("villages_fk_district_id", districtId, districts)(_.id)
	}
}

@ImplementedBy(classOf[VillagesDaoImpl])
trait VillagesDao {
	def findById(id: Int): Future[Option[Village]]
	def findAll(): Future[Seq[Village]]
}

@Singleton
class VillagesDaoImpl @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
	extends VillagesComponent
		with VillagesDao
		with HasDatabaseConfigProvider[JdbcProfile]
		with LazyLogging {

	import dbConfig.profile.api._

	val villages = TableQuery[Villages]

	override def findById(id: Int) = {
		db.run {
			villages.filter(_.id === id).result.headOption
		}
	}

	override def findAll(): Future[Seq[Village]] = {
		db.run(villages.result)
	}
}