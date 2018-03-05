package models

import java.util.Date

import play.api.libs.json.Json

object CheckUpProtocol {

	case class CheckUp(
    id: Option[Int] = None,
    patientId: Option[Int] = None,
    userId: Option[Int] = None,
    startedAt: Option[Date] = None,
    finishedAt: Option[Date] = None,
    complaint: Option[String] = None,
    objInfo: Option[String] = None,
    objReview: Option[String] = None,
    statusLocalis: Option[String] = None,
    diagnose: Option[String] = None,
    recommendation: Option[String] = None,
	)

	implicit val checkUpFormat = Json.format[CheckUp]

	case class AddCheckUp(checkUp: CheckUp, filePaths: Seq[String])

}
