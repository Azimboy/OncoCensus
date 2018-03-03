package models.daos

import java.util.Date
import javax.inject.{Inject, Singleton}

import com.google.inject.ImplementedBy
import com.typesafe.scalalogging.LazyLogging
import models.MedicalCheckProtocol.MedicalCheck
import models.utils.Date2SqlDate
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.Future

trait MedicalChecksComponent
	extends PatientsComponent
	with UsersComponent
	with Date2SqlDate
{ self: HasDatabaseConfigProvider[JdbcProfile] =>

	import dbConfig.profile.api._

	class MedicalChecks(tag: Tag) extends Table[MedicalCheck](tag, "medical_checks") {
		val patients = TableQuery[Patients]
		val users = TableQuery[Users]

		def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
		def patientId = column[Int]("patient_id")
		def userId = column[Int]("user_id")
		def startedAt = column[Date]("started_at")
		def finishedAt = column[Date]("finished_at")
		def objInfo = column[String]("obj_info")
		def objReview = column[String]("obj_review")
		def statusLokalis = column[String]("status_lokalis")
		def diagnose = column[String]("diagnose")
		def recommendation = column[String]("recommendation")

		def * = (id.?, patientId.?, userId.?, startedAt.?, finishedAt.?, objInfo.?, objReview.?, statusLokalis.?, diagnose.?, recommendation.?) <>
			(MedicalCheck.tupled, MedicalCheck.unapply _)

		def patient = foreignKey("medical_checks_fk_patient_id", patientId, patients)(_.id)
		def user = foreignKey("medical_checks_fk_user_id", userId, users)(_.id)
	}
}

@ImplementedBy(classOf[MedicalChecksDaoImpl])
trait MedicalChecksDao {
	def findById(id: Int): Future[Option[MedicalCheck]]
}

@Singleton
class MedicalChecksDaoImpl @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
	extends MedicalChecksDao
		with MedicalChecksComponent
		with PatientsComponent
		with UsersComponent
		with HasDatabaseConfigProvider[JdbcProfile]
		with LazyLogging {

	import dbConfig.profile.api._

	val medicalChecks = TableQuery[MedicalChecks]

	override def findById(id: Int) = {
		db.run {
			medicalChecks.filter(_.id === id).result.headOption
		}
	}

}