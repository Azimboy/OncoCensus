package models

import java.util.Date

import play.api.libs.json.Json

object MedicalCheckProtocol {

	case class MedicalCheck(
    id: Option[Int] = None,
    patientId: Option[Int] = None,
    userId: Option[Int] = None,
    startedAt: Option[Date] = None,
    finishedAt: Option[Date] = None,
    complaint: Option[String],
    objInfo: Option[String] = None,
    objReview: Option[String] = None,
    statusLocalis: Option[String] = None,
    diagnose: Option[String] = None,
    recommendation: Option[String] = None,
	)

	implicit val medicalCheckFormat = Json.format[MedicalCheck]

}
