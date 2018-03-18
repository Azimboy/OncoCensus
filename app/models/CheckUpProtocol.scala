package models

import java.util.Date

import models.UserProtocol.User
import play.api.libs.json.Json

object CheckUpProtocol {

	case class CheckUpFile(
    id: Option[Int] = None,
    checkUpId: Option[Int] = None,
    uploadedAt: Option[Date] = None,
    fileId: Option[String] = None,
    originalFileName: Option[String] = None
  )

	case class CheckUp(
    id: Option[Int] = None,
    patientId: Option[Int] = None,
    userId: Option[Int] = None,
    createdAt: Option[Date] = None,
    startedAt: Option[Date] = None,
    finishedAt: Option[Date] = None,
    complaint: Option[String] = None,
    objInfo: Option[String] = None,
    objReview: Option[String] = None,
    statusLocalis: Option[String] = None,
    diagnose: Option[String] = None,
    recommendation: Option[String] = None,
    user: Option[User] = None,
    files: Seq[CheckUpFile] = Nil
	)

	implicit val checkUpFileFormat = Json.format[CheckUpFile]
	implicit val checkUpFormat = Json.format[CheckUp]

	case class ModifyCheckUp(checkUp: CheckUp, filePaths: Seq[String])

	case class GetCheckUpsByPatientId(patientId: Int)

}
