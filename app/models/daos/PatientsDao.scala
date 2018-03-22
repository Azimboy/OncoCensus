package models.daos

import java.util.Date
import javax.inject.{Inject, Singleton}

import com.google.inject.ImplementedBy
import com.typesafe.scalalogging.LazyLogging
import models.AppProtocol.ReportData
import models.PatientProtocol.{Gender, Patient, PatientsFilter}
import models.utils.{Date2SqlDate, DateUtils}
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
    def clientGroupId = column[Int]("client_group_id")
    def patientDataJson = column[JsValue]("patient_data_json")
    def supervisedOutJson = column[JsValue]("supervised_out_json")

    def * = (id.?, createdAt.?, deletedAt.?, firstNameEncr.?, lastNameEncr.?, middleNameEncr.?, gender.?, birthDate.?,
       districtId.?, emailEncr.?, phoneNumberEncr.?, avatarId.?, clientGroupId.?, patientDataJson.?, supervisedOutJson.?).shaped <>
      (t => {
        val fields =
          (t._1, t._2, t._3, t._4, t._5, t._6, t._7, t._8, t._9, t._10, t._11, t._12, t._13, t._14, t._15, None, None)
        (Patient.apply _).tupled(fields)
      },
        (i: Patient) =>
          Patient.unapply(i).map { t =>
            (t._1, t._2, t._3, t._4, t._5, t._6, t._7, t._8, t._9, t._10, t._11, t._12, t._13, t._14, t._15)
          }
      )

    def district = foreignKey("patients_fk_district_id", districtId, districts)(_.id)
    def clientGroup = foreignKey("patients_fk_client_group_id", clientGroupId, clientGroups)(_.id)
  }
}

@ImplementedBy(classOf[PatientsImpl])
trait PatientsDao {
  def create(patient: Patient): Future[Int]
  def update(patient: Patient): Future[Int]
  def delete(patientId: Int): Future[Int]
  def findByFilter(patientsFilter: PatientsFilter): Future[Seq[Patient]]
  def findById(patientId: Int): Future[Option[Patient]]
}

@Singleton
class PatientsImpl @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
                                (implicit val ec: ExecutionContext)
  extends PatientsDao
  with PatientsComponent
  with HasDatabaseConfigProvider[JdbcProfile]
  with Date2SqlDate
  with LazyLogging {

  import models.utils.PostgresDriver.api._

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

  override def update(patient: Patient): Future[Int] = {
    db.run(patients.filter(_.id === patient.id).update(patient))
  }

  override def delete(patientId: Int): Future[Int] = {
    db.run(patients.filter(_.id === patientId).delete)
  }

  override def findByFilter(patientsFilter: PatientsFilter): Future[Seq[Patient]] = {
    val byGender = (patientsFilter.isMale, patientsFilter.isFemale) match {
      case (true, false) => patients.filter(_.gender === Gender.Male)
      case (false, true) => patients.filter(_.gender === Gender.Female)
      case _ => patients
    }

    val minDate = getBirthDate(patientsFilter.minAge)
    val maxDate = getBirthDate(patientsFilter.maxAge)

    val byMaxAge = maxDate.map(t => byGender.filter(_.birthDate >= t)).getOrElse(byGender)
    val byMinAge = minDate.map(t => byMaxAge.filter(_.birthDate <= t)).getOrElse(byMaxAge)

    val withDistricts = byMinAge.join(districts).on(_.districtId === _.id)

    val byRegion = patientsFilter.regionId match {
      case Some(regionId) => withDistricts.filter(_._2.regionId === regionId)
      case None => withDistricts
    }

    val byDistrict = patientsFilter.districtId match {
      case Some(districtId) => byRegion.filter(_._1.districtId === districtId)
      case None => byRegion
    }

    val byClientGroup = patientsFilter.clientGroupId match {
      case Some(clientGroupId) => byDistrict.filter(_._1.clientGroupId === clientGroupId)
      case None => byDistrict
    }

    val withClientGroup = byClientGroup.join(clientGroups).on(_._1.clientGroupId === _.id)

    db.run {
      withClientGroup.sortBy(_._1._1.id).result
    }.map(_.map { case ((patient, district), clientGroup) =>
      patient.copy(district = Some(district), clientGroup = Some(clientGroup))
    })
  }

  override def findById(patientId: Int): Future[Option[Patient]] = {
    db.run(patients.filter(_.id === patientId).result.headOption)
  }

  private def getBirthDate(ageOpt: Option[Int]) = {
    ageOpt.map(age => DateUtils.addYearsToCurrentDate(-age))
  }

}