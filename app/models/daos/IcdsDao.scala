package models.daos

import com.typesafe.scalalogging.LazyLogging
import models.PatientProtocol.Icd
import models.utils.db.DatabaseConnector

import scala.concurrent.{ExecutionContext, Future}

trait IcdsComponent {
  import models.utils.PostgresDriver.api._

	class Icds(tag: Tag) extends Table[Icd](tag, "icds") {
		def code = column[String]("code", O.PrimaryKey)
		def name = column[String]("name")
		def * = (code, name.?) <>
			(Icd.tupled, Icd.unapply _)
	}
}

sealed trait IcdsDao {
	def create(icd: Icd): Future[String]
	def findByCode(code: String): Future[Option[Icd]]
	def getAllIcds(): Future[Seq[Icd]]
}

class IcdsDaoImpl(val databaseConnector: DatabaseConnector)
                 (implicit executionContext: ExecutionContext)
	extends IcdsDao
		with IcdsComponent
		with LazyLogging {

  import databaseConnector._
  import databaseConnector.profile.api._

	val icds = TableQuery[Icds]

	override def create(icd: Icd): Future[String] = {
		db.run {
			(icds returning icds.map(_.code)) += icd
		}
	}

	override def findByCode(code: String) = {
		db.run {
			icds.filter(_.code === code).result.headOption
		}
	}

	override def getAllIcds(): Future[Seq[Icd]] = {
		db.run(icds.result)
	}
}