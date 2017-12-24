package models.daos

import javax.inject.{Inject, Singleton}

import com.google.inject.ImplementedBy
import com.typesafe.scalalogging.LazyLogging
import models.PatientProtocol.ClientGroup
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.Future

trait ClientGroupsComponent
{ self: HasDatabaseConfigProvider[JdbcProfile] =>

	import dbConfig.profile.api._

	class ClientGroups(tag: Tag) extends Table[ClientGroup](tag, "client_groups") {
		def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
		def name = column[String]("name")
		def code = column[String]("code")
		def * = (id.?, name.?, code.?) <>
			(ClientGroup.tupled, ClientGroup.unapply _)
	}
}

@ImplementedBy(classOf[ClientGroupsDaoImpl])
trait ClientGroupsDao {
	def findById(id: Int): Future[Option[ClientGroup]]
	def getAllClientGroups(): Future[Seq[ClientGroup]]
}

@Singleton
class ClientGroupsDaoImpl @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
	extends ClientGroupsDao
		with ClientGroupsComponent
		with HasDatabaseConfigProvider[JdbcProfile]
		with LazyLogging {

	import dbConfig.profile.api._

	val clientGroups = TableQuery[ClientGroups]

	override def findById(id: Int) = {
		db.run {
			clientGroups.filter(_.id === id).result.headOption
		}
	}

	override def getAllClientGroups(): Future[Seq[ClientGroup]] = {
		db.run(clientGroups.result)
	}
}