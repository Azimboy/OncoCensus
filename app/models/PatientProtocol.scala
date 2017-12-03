package models

import java.util.Date

import models.AppProtocol.District
import models.utils.{EnumMappedToDb, EnumUtils}
import play.api.libs.json.{JsValue, Json}

object PatientProtocol {

  case class Patient(
    id: Option[Int] = None,
    createdAt: Option[Date] = None,
    updatedAt: Option[Date] = None,
    firstName: Option[String] = None,
    lastName: Option[String] = None,
    middleName: Option[String] = None,
    gender: Option[Gender.Value] = None,
    birthDate: Option[Date] = None,
    districtId: Option[Int] = None,
    email: Option[String] = None,
    phoneNumber: Option[String] = None,
    patientDataJson: Option[JsValue] = None,
    district: Option[District] = None,
  )

  object Gender extends EnumMappedToDb {
    val Man = Value(1)
    val Woman = Value(0)

    def withShortName: PartialFunction[String, Gender.Value] = {
      case "man" => Man
      case "woman" => Woman
    }
  }

  case class PatientData(
    passportNo: Option[String] = None,
    province: Option[String] = None,
    street: Option[String] = None,
    home: Option[String] = None,
    work: Option[String] = None,
    position: Option[String] = None,
    bloodGroup: Option[String] = None
  )

  implicit val genderFormat = EnumUtils.enumFormat(Gender)
  implicit val patientDataFormat = Json.format[PatientData]
  implicit val patientFormat = Json.format[Patient]

  case class AddPatient(patient: Patient)
}
