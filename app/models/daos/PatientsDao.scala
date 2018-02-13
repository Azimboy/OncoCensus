package models.daos

import java.util.Date
import javax.inject.{Inject, Singleton}

import com.google.inject.ImplementedBy
import models.PatientProtocol.{Gender, Patient}
import models.utils.Date2SqlDate
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.json.JsValue
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

trait PatientsComponent extends DistrictsComponent with ClientGroupsComponent
  { self: HasDatabaseConfigProvider[JdbcProfile] =>

  import models.utils.PostgresDriver.api._

  class Patients(tag: Tag) extends Table[Patient](tag, "patients") with Date2SqlDate {
    val districts = TableQuery[Districts]
    val clientGroups = TableQuery[ClientGroups]

    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def createdAt = column[Date]("created_at")
    def deletedAt = column[Date]("deleted_at")
    def firstNameEncr = column[String]("first_name_encr")
    def lastNameEncr = column[String]("last_name_encr")
    def middleNameEncr = column[String]("middle_name_encr")
    def gender = column[Gender.Value]("gender")
    def birthDate = column[Date]("birth_date")
    def districtId = column[Int]("district_id")
    def emailEncr = column[String]("email_encr")
    def phoneNumberEncr = column[String]("phone_number_encr")
    def avatarId = column[String]("avatar_id")
    def patientDataJson = column[JsValue]("patient_data_json")
    def clientGroupId = column[Int]("client_group_id")
    def deadAt = column[Date]("dead_at")
    def deadReason = column[String]("dead_reason")

    def * = (id.?, createdAt.?, deletedAt.?, firstNameEncr.?, lastNameEncr.?, middleNameEncr.?, gender.?, birthDate.?,
       districtId.?, emailEncr.?, phoneNumberEncr.?, avatarId.?, patientDataJson.?, clientGroupId.?, deadAt.?, deadReason.?).shaped <>
      (t => {
        val fields =
          (t._1, t._2, t._3, t._4, t._5, t._6, t._7, t._8, t._9, t._10, t._11, t._12, t._13, t._14, t._15, t._16, None, None)
        (Patient.apply _).tupled(fields)
      },
        (i: Patient) =>
          Patient.unapply(i).map { t =>
            (t._1, t._2, t._3, t._4, t._5, t._6, t._7, t._8, t._9, t._10, t._11, t._12, t._13, t._14, t._15, t._16)
          }
      )

    def district = foreignKey("patients_fk_district_id", districtId, districts)(_.id)
    def clientGroup = foreignKey("patients_fk_client_group_id", clientGroupId, clientGroups)(_.id)
  }
}

@ImplementedBy(classOf[PatientsImpl])
trait PatientsDao {
  def create(Patient: Patient): Future[Int]
  def findAll: Future[Seq[Patient]]
}

@Singleton
class PatientsImpl @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
                                (implicit val ec: ExecutionContext)
  extends PatientsDao
  with PatientsComponent
  with HasDatabaseConfigProvider[JdbcProfile]
  with Date2SqlDate {

  import dbConfig.profile.api._

  val patients = TableQuery[Patients]
  val districts = TableQuery[Districts]
  val clientGroups = TableQuery[ClientGroups]

  override def create(Patient: Patient) = {
    db.run {
      (patients returning patients.map(_.id)
        into ((r, id) => id)
        ) += Patient
    }
  }

  override def findAll(): Future[Seq[Patient]] = {
    db.run {
      patients
        .join(districts).on(_.districtId === _.id)
        .join(clientGroups).on(_._1.clientGroupId === _.id)
        .result
    }.map(_.map { case ((patient, district), clientGroup) =>
      patient.copy(district = Some(district), clientGroup = Some(clientGroup))
    })
  }

}