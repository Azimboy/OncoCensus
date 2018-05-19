package models.daos

import java.util.Date

import com.google.inject.ImplementedBy
import com.typesafe.scalalogging.LazyLogging
import javax.inject.{Inject, Singleton}
import models.PatientProtocol.{Gender, Patient, PatientsFilter}
import models.utils.{Date2SqlDate, DateUtils}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.json.JsValue
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

trait PatientsComponent extends VillagesComponent with IcdsComponent
  { self: HasDatabaseConfigProvider[JdbcProfile] =>

  import models.utils.PostgresDriver.api._

  class Patients(tag: Tag) extends Table[Patient](tag, "patients") with Date2SqlDate {
    val villages = TableQuery[Villages]
    val icds = TableQuery[Icds]

    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def createdAt = column[Date]("created_at")
    def deletedAt = column[Date]("deleted_at")
    def firstNameEncr = column[String]("first_name_encr")
    def lastNameEncr = column[String]("last_name_encr")
    def middleNameEncr = column[String]("middle_name_encr")
    def passportId = column[String]("passport_id")
    def gender = column[Gender.Value]("gender")
    def birthDate = column[Date]("birth_date")
    def villageId = column[Int]("village_id")
    def icdId = column[Int]("icd_id")
    def avatarId = column[String]("avatar_id")
    def patientDataJson = column[JsValue]("patient_data_json")
    def supervisedOutJson = column[JsValue]("supervised_out_json")

    def * = (id.?, createdAt.?, deletedAt.?, firstNameEncr.?, lastNameEncr.?, middleNameEncr.?, passportId, gender, birthDate,
       villageId, icdId, avatarId.?, patientDataJson.?, supervisedOutJson.?).shaped <>
      (t => {
        val fields =
          (t._1, t._2, t._3, t._4, t._5, t._6, t._7, t._8, t._9, t._10, t._11, t._12, t._13, t._14, None, None)
        (Patient.apply _).tupled(fields)
      },
        (i: Patient) =>
          Patient.unapply(i).map { t =>
            (t._1, t._2, t._3, t._4, t._5, t._6, t._7, t._8, t._9, t._10, t._11, t._12, t._13, t._14)
          }
      )

    def village = foreignKey("patients_fk_village_id", villageId, villages)(_.id)
    def icd = foreignKey("patients_fk_icd_id", icdId, icds)(_.id)
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
  val villages = TableQuery[Villages]
  val icds = TableQuery[Icds]

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

    val withVillages = byMinAge.join(villages).on(_.villageId === _.id)
    val withDistricts = withVillages.join(districts).on(_._2.districtId === _.id)

    val byRegion = patientsFilter.regionId match {
      case Some(regionId) => withDistricts.filter(_._2.regionId === regionId)
      case None => withDistricts
    }

    val byDistrict = patientsFilter.districtId match {
      case Some(districtId) => byRegion.filter(_._1._2.districtId === districtId)
      case None => byRegion
    }

    val byIcd = patientsFilter.icdId match {
      case Some(icdId) => byDistrict.filter(_._1._1.icdId === icdId)
      case None => byDistrict
    }

    val byPassportId = patientsFilter.passportId match {
      case Some(passportId) => byIcd.filter(_._1._1.passportId === passportId)
      case None => byIcd
    }

    val withIcd = byPassportId.join(icds).on(_._1._1.icdId === _.id)

    db.run {
      withIcd.sortBy(_._1._1._1.id).result
    }.map(_.map { case (((patient, villages), _), icd) =>
      patient.copy(village = Some(villages), icd = Some(icd))
    })
  }

  override def findById(patientId: Int): Future[Option[Patient]] = {
    db.run(patients.filter(_.id === patientId).result.headOption)
  }

  private def getBirthDate(ageOpt: Option[Int]) = {
    ageOpt.map(age => DateUtils.addYearsToCurrentDate(-age))
  }

}