package models

import java.util.Date

import models.UserProtocol.User
import models.PatientProtocol._
import models.utils.{EnumMappedToDb, EnumUtils}
import play.api.libs.json.{JsValue, Json}

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
    receiveInfoJson: Option[JsValue] = None,
    user: Option[User] = None,
    files: Seq[CheckUpFile] = Nil
	)

	case class ReceiveInfo(
    receiveType: ReceiveType.Value,
    receiveReason: ReceiveReason.Value,
  )

	object ReceiveType extends EnumMappedToDb {
		val Polyclinic = Value("polyclinic")
		val Home = Value("home")

		def withShortName: PartialFunction[String, ReceiveType.Value] = {
			case "polyclinic" => Polyclinic
			case "home" => Home
		}
	}

	object ReceiveReason extends EnumMappedToDb {
		val Simple = Value("simple")
		val Illness = Value("illness")

		def withShortName: PartialFunction[String, ReceiveReason.Value] = {
			case "simple" => Simple
			case "illness" => Illness
		}
	}

	implicit val receiveTypeFormat = EnumUtils.enumFormat(ReceiveType)
	implicit val receiveReasonFormat = EnumUtils.enumFormat(ReceiveReason)
	implicit val receiveInfoFormat = Json.format[ReceiveInfo]
	implicit val checkUpFileFormat = Json.format[CheckUpFile]
	implicit val checkUpFormat = Json.format[CheckUp]

	case class ModifyCheckUp(checkUp: CheckUp, filePaths: Seq[String])

	case class GetCheckUpsByPatientId(patientId: Int)

}
