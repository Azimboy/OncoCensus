package models

import java.nio.file.Path
import java.util.Date

import models.AppProtocol.Paging.PageReq
import models.AppProtocol.{District, _}
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

  case class Icd(
    id: Option[Int] = None,
    name: Option[String] = None,
    code: Option[String] = None
  )

  case class PatientData(
    province: Option[String] = None,
    street: Option[String] = None,
    home: Option[String] = None,
    work: Option[String] = None,
    position: Option[String] = None,
    bloodGroup: Option[BloodGroup.Value] = None,
    email: Option[String] = None,
    phoneNumber: Option[String] = None
  )

  object BloodGroup extends EnumMappedToDb {
    val I_Plus = Value("I(+)")
    val I_Minus = Value("I(-)")
    val II_Plus = Value("II(+)")
    val II_Minus = Value("II(-)")
    val III_Plus = Value("III(+)")
    val III_Minus = Value("III(-)")
    val IV_Plus = Value("IV(+)")
    val IV_Minus = Value("IV(-)")

    def withShortName: PartialFunction[String, BloodGroup.Value] = {
      case "I(+)" => I_Plus
      case "I(-)" => I_Minus
      case "II(+)" => II_Plus
      case "II(-)" => II_Minus
      case "III(+)" => III_Plus
      case "III(-)" => III_Minus
      case "IV(+)" => IV_Plus
      case "IV(-)" => IV_Minus
    }
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
  implicit val bloodGroupFormat = EnumUtils.enumFormat(BloodGroup)
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
