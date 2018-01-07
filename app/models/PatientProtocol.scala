package models

import java.util.Date

import models.AppProtocol.District
import models.utils.{EnumMappedToDb, EnumUtils}
import play.api.libs.json.{JsValue, Json}

object PatientProtocol {

  case class Patient(
    id: Option[Int] = None,
    createdAt: Option[Date] = None,
    deletedAt: Option[Date] = None,
    firstName: Option[String] = None,
    lastName: Option[String] = None,
    middleName: Option[String] = None,
    gender: Option[Gender.Value] = None,
    birthDate: Option[Date] = None,
    districtId: Option[Int] = None,
    email: Option[String] = None,
    phoneNumber: Option[String] = None,
    patientDataJson: Option[JsValue] = None,
    clientGroupId: Option[Int] = None,
    deadAt: Option[Date] = None,
    deadReason: Option[String] = None,
    district: Option[District] = None,
    clientGroup: Option[ClientGroup] = None
  )

  object Gender extends EnumMappedToDb {
    val Male = Value(1)
    val Female = Value(0)

    def withShortName: PartialFunction[String, Gender.Value] = {
      case "male" => Male
      case "female" => Female
    }
  }

  case class ClientGroup(
    id: Option[Int] = None,
    name: Option[String] = None,
    code: Option[String] = None
  )

  case class PatientData(
    passportNo: Option[String] = None,
    province: Option[String] = None,
    street: Option[String] = None,
    home: Option[String] = None,
    work: Option[String] = None,
    position: Option[String] = None,
    bloodGroup: Option[BloodGroup.Value] = None
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

  implicit val genderFormat = EnumUtils.enumFormat(Gender)
  implicit val bloodGroupFormat = EnumUtils.enumFormat(BloodGroup)
  implicit val clientGroupFormat = Json.format[ClientGroup]
  implicit val patientDataFormat = Json.format[PatientData]
  implicit val patientFormat = Json.format[Patient]

  case class AddPatient(patient: Patient)

  case object GetAllPatients
  case object GetAllClientGroups
}
