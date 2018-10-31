package models.daos

import java.util.Date

import com.google.inject.ImplementedBy
import com.typesafe.scalalogging.LazyLogging
import javax.inject.{Inject, Singleton}
import models.CheckUpProtocol.CheckUpFile
import models.utils.Date2SqlDate
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.Future

trait CheckUpFilesComponent
  extends CheckUpsComponent
    with Date2SqlDate { self: HasDatabaseConfigProvider[JdbcProfile] =>
	import models.utils.PostgresDriver.api._

	class CheckUpFiles(tag: Tag) extends Table[CheckUpFile](tag, "check_up_files") {
		val checkUps = TableQuery[CheckUps]

		def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
		def checkUpId = column[Int]("check_up_id")
		def uploadedAt = column[Date]("uploaded_at")
		def fileId = column[String]("file_id")
		def originalFileName = column[String]("original_file_name")

		def * = (id.?, checkUpId.?, uploadedAt.?, fileId.?, originalFileName.?) <>
			(CheckUpFile.tupled, CheckUpFile.unapply _)

		def checkUp = foreignKey("check_up_files_fk_check_up_id", checkUpId, checkUps)(_.id)
	}
}

@ImplementedBy(classOf[CheckUpFilesDaoImpl])
trait CheckUpFilesDao {
	def create(checkUpFile: CheckUpFile): Future[Int]
}

@Singleton
class CheckUpFilesDaoImpl @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
	extends CheckUpFilesDao
		with CheckUpFilesComponent
		with CheckUpsComponent
		with HasDatabaseConfigProvider[JdbcProfile]
		with LazyLogging {

	import dbConfig.profile.api._

	val checkUps = TableQuery[CheckUps]
	val checkUpFiles = TableQuery[CheckUpFiles]

	override def create(checkUpFile: CheckUpFile): Future[Int] = {
		db.run {
			(checkUpFiles returning checkUpFiles.map(_.id)
				into ((r, id) => id)
				) += checkUpFile
		}
	}

}