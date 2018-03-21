package models.daos

import java.util.Date
import javax.inject.{Inject, Singleton}

import com.google.inject.ImplementedBy
import com.typesafe.scalalogging.LazyLogging
import models.AppProtocol.ReportData
import models.CheckUpProtocol.CheckUp
import models.UserProtocol.User
import models.utils.Date2SqlDate
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.json.JsValue
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

trait CheckUpsComponent
	extends PatientsComponent
	with UsersComponent
	with Date2SqlDate
{ self: HasDatabaseConfigProvider[JdbcProfile] =>

	import models.utils.PostgresDriver.api._

	class CheckUps(tag: Tag) extends Table[CheckUp](tag, "check_ups") {
		val patients = TableQuery[Patients]
		val users = TableQuery[Users]

		def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
		def patientId = column[Int]("patient_id")
		def userId = column[Int]("user_id")
		def createdAt = column[Date]("created_at")
		def startedAt = column[Date]("started_at")
		def finishedAt = column[Date]("finished_at")
		def complaint = column[String]("complaint")
		def objInfo = column[String]("obj_info")
		def objReview = column[String]("obj_review")
		def statusLocalis = column[String]("status_localis")
		def diagnose = column[String]("diagnose")
		def recommendation = column[String]("recommendation")
		def receiveInfoJson = column[JsValue]("receive_info_json")

		def * = (id.?, patientId.?, userId.?, createdAt.?, startedAt.?, finishedAt.?, complaint.?,
			objInfo.?, objReview.?, statusLocalis.?, diagnose.?, recommendation.?, receiveInfoJson.?) <>
			(t => {
				val fields =
					(t._1, t._2, t._3, t._4, t._5, t._6, t._7, t._8, t._9, t._10, t._11, t._12, t._13, None, None, Nil)
				(CheckUp.apply _).tupled(fields)
			},
				(i: CheckUp) =>
					CheckUp.unapply(i).map { t =>
						(t._1, t._2, t._3, t._4, t._5, t._6, t._7, t._8, t._9, t._10, t._11, t._12, t._13)
					}
			)

		def patient = foreignKey("check_ups_fk_patient_id", patientId, patients)(_.id)
		def user = foreignKey("check_ups_fk_user_id", userId, users)(_.id)
	}
}

@ImplementedBy(classOf[CheckUpsDaoImpl])
trait CheckUpsDao {
	def create(checkUp: CheckUp): Future[Int]
	def update(checkUp: CheckUp): Future[Int]
	def findByPatientId(patientId: Int): Future[Seq[CheckUp]]
	def findById(id: Int): Future[Option[CheckUp]]
	def getAllCheckUps(reportData: ReportData): Future[Seq[CheckUp]]
}

@Singleton
class CheckUpsDaoImpl @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
                               (implicit val ec: ExecutionContext)
	extends CheckUpsDao
		with CheckUpsComponent
		with PatientsComponent
		with UsersComponent
		with HasDatabaseConfigProvider[JdbcProfile]
		with LazyLogging {

	import models.utils.PostgresDriver.api._

	val checkUps = TableQuery[CheckUps]
	val patients = TableQuery[Patients]
	val districts = TableQuery[Districts]
	val users = TableQuery[Users]

	override def create(checkUp: CheckUp): Future[Int] = {
		db.run {
			(checkUps returning checkUps.map(_.id)
				into ((r, id) => id)
				) += checkUp
		}
	}

	override def update(checkUp: CheckUp): Future[Int] = {
		db.run(checkUps.filter(_.id === checkUp.id).update(checkUp))
	}

	override def findByPatientId(patientId: Int): Future[Seq[CheckUp]] = {
		db.run {
			checkUps.filter(_.patientId === patientId)
				.join(users).on(_.userId === _.id).result
		}.map(_.map { case (checkUp, user) =>
			checkUp.copy(user = Some(user))
		})
	}

	override def findById(id: Int) = {
		db.run {
			checkUps.filter(_.id === id).result.headOption
		}
	}

	override def getAllCheckUps(reportData: ReportData): Future[Seq[CheckUp]] = {
		val withPatients = checkUps.join(patients).on(_.patientId === _.id)
		val withDistricts = withPatients.join(districts).on(_._2.districtId === _.id)

		val byStartDate = reportData.startDate.map(t => withDistricts.filter(_._1._1.createdAt >= t)).getOrElse(withDistricts)
		val byEndDate = reportData.endDate.map(t => byStartDate.filter(_._1._1.createdAt <= t)).getOrElse(byStartDate)

		val byRegion = reportData.regionId.map(r => byEndDate.filter(_._2.regionId === r)).getOrElse(byEndDate)
		val byDistrict = reportData.districtId.map(d => byRegion.filter(_._2.id === d)).getOrElse(byRegion)

		val byReceiveType = reportData.receiveType.map { r =>
			byDistrict.filter(_._1._1.receiveInfoJson.+>>("receiveType") === r)
		}.getOrElse {
			byDistrict
		}

		db.run {
			byReceiveType.result
		}.map(_.map { case ((checkUp, patient), _) =>
			checkUp.copy(patient = Some(patient))
		})
	}

}