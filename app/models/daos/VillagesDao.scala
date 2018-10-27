package models.daos

import com.typesafe.scalalogging.LazyLogging
import models.AppProtocol.Village
import models.utils.db.DatabaseConnector

import scala.concurrent.{ExecutionContext, Future}

trait VillagesComponent extends DistrictsComponent {
	import models.utils.PostgresDriver.api._

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

sealed trait VillagesDao {
	def findById(id: Int): Future[Option[Village]]
	def findAll(): Future[Seq[Village]]
}

class VillagesDaoImpl(val databaseConnector: DatabaseConnector)
										 (implicit executionContext: ExecutionContext)
	extends VillagesDao
		with VillagesComponent
		with LazyLogging {

	import databaseConnector._
	import databaseConnector.profile.api._

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