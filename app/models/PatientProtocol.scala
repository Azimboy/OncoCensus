package models

import java.nio.file.Path
import java.util.Date

import models.AppProtocol.Paging.PageReq
import models.AppProtocol._
import models.utils.{EnumMappedToDb, EnumUtils}
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._

object PatientProtocol {

  case class Patient(
    id: Option[Int] = None,
    createdAt: Option[Date] = None,
    deletedAt: Option[Date] = None,
    firstName: Option[String] = None,
    lastName: Option[String] = None,
    middleName: Option[String] = None,
    passportId: String,
    gender: Gender.Value,
    birthDate: Date,
    villageId: Int,
    icdId: Int,
    clientGroup: ClientGroup.Value,
    avatarId: Option[String] = None,
    patientDataJson: Option[JsValue] = None,
    supervisedOutJson: Option[JsValue] = None,
    village: Option[Village] = None,
    icd: Option[Icd] = None
  )

  object Gender extends EnumMappedToDb {
    val Male = Value(1)
    val Female = Value(0)

    def withShortName: PartialFunction[String, Gender.Value] = {
      case "Male" => Male
      case "Female" => Female
    }
  }

  case class PatientData(
    province: Option[String] = None,
    street: Option[String] = None,
    home: Option[String] = None,
    work: Option[String] = None,
    position: Option[String] = None,
    bloodType: Option[BloodType.Value] = None,
    email: Option[String] = None,
    phoneNumber: Option[String] = None
  )


  case class Icd(
    id: Option[Int] = None,
    name: Option[String] = None,
    code: Option[String] = None
  )

  object ClientGroup extends EnumMappedToDb {
    val I = Value("1")
    val II = Value("2")
    val III = Value("3")
    val IV = Value("4")

    def withShortName: PartialFunction[String, ClientGroup.Value] = {
      case "1" => I
      case "2" => II
      case "3" => III
      case "4" => IV
    }
  }

  object BloodType extends EnumMappedToDb {
    val I_- = Value("O(I) Rh−")
    val I_+ = Value("O(I) Rh+")
    val II_- = Value("A(II) Rh−")
    val II_+ = Value("A(II) Rh+")
    val III_- = Value("B(III) Rh−")
    val III_+ = Value("B(III) Rh+")
    val IV_- = Value("AB(IV) Rh−")
    val IV_+ = Value("AB(IV) Rh+")

    def withShortName: PartialFunction[String, BloodType.Value] = {
      case "O(I) Rh−" => I_-
      case "O(I) Rh+" => I_+
      case "A(II) Rh−" => II_-
      case "A(II) Rh+" => II_+
      case "B(III) Rh−" => III_-
      case "B(III) Rh+" => III_+
      case "AB(IV) Rh−" => IV_-
      case "AB(IV) Rh+" => IV_+
    }

    val all = Seq(I_-, I_+, II_-, II_+, III_-, III_+, IV_-, IV_+)
  }

  object SupervisedOutReason extends EnumMappedToDb {
    val Recovery = Value("recovery")
    val Dead = Value("dead")

    def withShortName: PartialFunction[String, SupervisedOutReason.Value] = {
      case "recovery" => Recovery
      case "dead" => Dead
    }
  }

  case class PatientsFilter(
    lastName: Option[String],
    isMale: Boolean,
    isFemale: Boolean,
    minAge: Option[Int],
    maxAge: Option[Int],
    regionId: Option[Int],
    districtId: Option[Int],
    icdId: Option[Int],
    passportId: Option[String],
    province: Option[String]
  )

  implicit val patientsFilterFormat = Json.format[PatientsFilter]
  implicit val genderFormat = EnumUtils.enumFormat(Gender)
  implicit val bloodTypeFormat = EnumUtils.enumFormat(BloodType)
  implicit val clientGroupFormat = EnumUtils.enumFormat(ClientGroup)
  implicit val icdFormat = Json.format[Icd]
  implicit val patientDataFormat = Json.format[PatientData]
  implicit val patientFormat = Json.format[Patient]
  implicit val supervisedOutReasonFormat = EnumUtils.enumFormat(SupervisedOutReason)

  case class ModifyPatient(patient: Patient, photosPath: Option[Path])
  case class DeletePatientById(patientId: Int)

  case class GetAllPatients(patientsFilter: PatientsFilter, pageReq: PageReq)
  case object GetAllIcds

  case class SupervisedOut(
    date: Date,
    reason: SupervisedOutReason.Value,
    comments: Option[String] = None
  )

  implicit val supervisedOutFormat: Format[SupervisedOut] = (
    dateFormat("date") ~
    (__ \ "reason").format[SupervisedOutReason.Value] ~
    (__ \ "comments").formatNullable[String]
  )(SupervisedOut.apply, unlift(SupervisedOut.unapply))

  case class PatientSupervisedOut(patientId: Int, supervisedOut: SupervisedOut)

}
