package models.daos

import java.util.Date
import javax.inject.{Inject, Singleton}

import com.google.inject.ImplementedBy
import com.typesafe.scalalogging.LazyLogging
import models.CheckUpProtocol.CheckUp
import models.UserProtocol.User
import models.utils.Date2SqlDate
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.Future

trait CheckUpsComponent
	extends PatientsComponent
	with UsersComponent
	with Date2SqlDate
{ self: HasDatabaseConfigProvider[JdbcProfile] =>

	import dbConfig.profile.api._

	class CheckUps(tag: Tag) extends Table[CheckUp](tag, "check_ups") {
		val patients = TableQuery[Patients]
		val users = TableQuery[Users]

		def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
		def patientId = column[Int]("patient_id")
		def userId = column[Int]("user_id")
		def startedAt = column[Date]("started_at")
		def finishedAt = column[Date]("finished_at")
		def complaint = column[String]("complaint")
		def objInfo = column[String]("obj_info")
		def objReview = column[String]("obj_review")
		def statusLocalis = column[String]("status_localis")
		def diagnose = column[String]("diagnose")
		def recommendation = column[String]("recommendation")

		def * = (id.?, patientId.?, userId.?, startedAt.?, finishedAt.?, complaint.?, objInfo.?, objReview.?, statusLocalis.?, diagnose.?, recommendation.?) <>
			(CheckUp.tupled, CheckUp.unapply _)

		def patient = foreignKey("check_ups_fk_patient_id", patientId, patients)(_.id)
		def user = foreignKey("check_ups_fk_user_id", userId, users)(_.id)
	}
}

@ImplementedBy(classOf[CheckUpsDaoImpl])
trait CheckUpsDao {
	def create(checkUp: CheckUp): Future[Int]
	def findById(id: Int): Future[Option[CheckUp]]
}

@Singleton
class CheckUpsDaoImpl @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
	extends CheckUpsDao
		with CheckUpsComponent
		with PatientsComponent
		with UsersComponent
		with HasDatabaseConfigProvider[JdbcProfile]
		with LazyLogging {

	import dbConfig.profile.api._

	val checkUps = TableQuery[CheckUps]

	override def create(checkUp: CheckUp): Future[Int] = {
		db.run {
			(checkUps returning checkUps.map(_.id)
				into ((r, id) => id)
				) += checkUp
		}
	}

	override def findById(id: Int) = {
		db.run {
			checkUps.filter(_.id === id).result.headOption
		}
	}

}