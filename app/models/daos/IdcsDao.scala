package models.daos

import javax.inject.{Inject, Singleton}

import com.google.inject.ImplementedBy
import com.typesafe.scalalogging.LazyLogging
import models.PatientProtocol.Icd
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.Future

trait IcdsComponent
{ self: HasDatabaseConfigProvider[JdbcProfile] =>

	import dbConfig.profile.api._

	class Icds(tag: Tag) extends Table[Icd](tag, "icds") {
		def name = column[String]("name", O.PrimaryKey)
		def code = column[String]("code")
		def * = (code.?, name.?) <>
			(Icd.tupled, Icd.unapply _)
	}
}

@ImplementedBy(classOf[IcdsDaoImpl])
trait IcdsDao {
	def findByCode(code: String): Future[Option[Icd]]
	def getAllIcds(): Future[Seq[Icd]]
}

@Singleton
class IcdsDaoImpl @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
	extends IcdsDao
		with IcdsComponent
		with HasDatabaseConfigProvider[JdbcProfile]
		with LazyLogging {

	import dbConfig.profile.api._

	val icds = TableQuery[Icds]

	override def findByCode(code: String) = {
		db.run {
			icds.filter(_.code === code).result.headOption
		}
	}

	override def getAllIcds(): Future[Seq[Icd]] = {
		db.run(icds.result)
	}
}