package controllers

import java.nio.file.Paths
import java.text.SimpleDateFormat

import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.scalalogging.LazyLogging
import javax.inject._
import models.AppProtocol.Paging.{PageReq, PageRes}
import models.AppProtocol.{GetAllVillages, Village}
import models.CheckUpProtocol.{CheckUp, GetCheckUpsByPatientId, ModifyCheckUp, ReceiveInfo, ReceiveReason, ReceiveType}
import models.PatientProtocol._
import models.SimpleAuth
import models.utils.FileUtils.{SpreadsheetException, parseSpreadsheet}
import models.utils.StringUtils.cyril2Latin
import org.webjars.play.WebJarsUtil
import play.api.Configuration
import play.api.libs.Files.TemporaryFile
import play.api.libs.json.Json
import play.api.mvc._
import views.html.card_index

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@Singleton
class CardIndexController @Inject()(val controllerComponents: ControllerComponents,
                                    val configuration: Configuration,
                                    @Named("patient-manager") val patientManager: ActorRef,
                                    @Named("check-up-manager") val checkUpManager: ActorRef,
                                    @Named("department-manager") val departmentManager: ActorRef,
                                    implicit val actorSystem: ActorSystem,
                                    implicit val webJarsUtil: WebJarsUtil)
                                   (implicit val ec: ExecutionContext)
  extends BaseController
    with SimpleAuth
    with LazyLogging {

  implicit val defaultTimeout = Timeout(60.seconds)

  implicit def getValue(key: String)(implicit request: Request[MultipartFormData[TemporaryFile]]): Option[String] = {
    val value = request.body.dataParts.get(key).flatMap(_.headOption)

    if (value.forall(_.isEmpty)) {
      None
    } else {
      value
    }
  }

  def index = Action { implicit request => auth {
    Ok(card_index.index())
  }}

  def getPatients(page: Int, pageSize: Int) = Action.async(parse.json[PatientsFilter]) { implicit request => asyncAuth {
	  val pageReq = PageReq(page = page, size = pageSize)
    val patientsFilter = request.body
    (patientManager ? GetAllPatients(patientsFilter, pageReq)).mapTo[PageRes[Patient]].map { pageRes =>
      Ok(Json.toJson(pageRes))
    }.recover { case error =>
      logger.error("Error occurred during getting patients", error)
      InternalServerError
    }
  }}

  def modifyPatient = Action.async(parse.multipartFormData) { implicit request => asyncAuth {
	  val patientId = getValue("patientId").map(_.toInt)

	  val patientDataJs = Json.toJson(PatientData(
		  province = getValue("province").get,
		  street = "street",
		  home = "home",
		  work = "work",
		  position = "position",
		  bloodType = getValue("bloodType").map(BloodType.withName).get,
      email = "email",
      phoneNumber = "phoneNumber"
	  ))

	  val patient = Patient(
		  id = patientId,
		  firstName = "firstName",
		  lastName = "lastName",
		  middleName = "middleName",
      passportId = getValue("passportId").get,
      gender = getValue("gender").map(Gender.withShortName).get,
		  birthDate = parseDate(getValue("birthDate").get, "dd.MM.yyyy"),
		  villageId = getValue("villageId").map(_.toInt).get,
		  icd = getValue("icd").get,
		  clientGroup = getValue("clientGroup").map(ClientGroup.withShortName).get,
		  patientDataJson = Some(patientDataJs)
	  )

	  val photosPath = request.body.file("patientsPhoto").map(file => Paths.get(file.ref.getAbsolutePath))

    (patientManager ? ModifyPatient(patient, photosPath)).mapTo[Int].map { _ =>
      Ok("OK")
    }.recover { case error =>
      logger.error("Error occurred during creating new patient", error)
      InternalServerError
    }
  }}

  def deletePatient(patientId: Int) = Action.async { implicit request => asyncAuth {
    (patientManager ? DeletePatientById(patientId)).mapTo[Int].map { _ =>
      Ok(Json.toJson("OK"))
    }.recover { case error =>
      logger.error(s"Error occurred during deleting patient. PatientId: $patientId", error)
      InternalServerError
    }
  }}

  def modifyCheckUp = Action.async(parse.multipartFormData) { implicit request => asyncAuth {
	  val receivedInfoJs = Json.toJson(ReceiveInfo(
		  receiveType = getValue("receiveType").map(ReceiveType.withShortName).getOrElse(ReceiveType.Polyclinic),
		  receiveReason = getValue("receiveReason").map(ReceiveReason.withShortName).getOrElse(ReceiveReason.Simple)
	  ))

    val checkUp = CheckUp(
      id = getValue("checkUpId").map(_.toInt),
      patientId = getValue("patientId").map(_.toInt),
      userId = Some(1),
      startedAt = getValue("startedAt").map(d => parseDate(d)),
      finishedAt = getValue("finishedAt").map(d => parseDate(d)),
      complaint = "complaint",
      objInfo = "objInfo",
      objReview = "objReview",
      statusLocalis = "statusLocalis",
      diagnose = "diagnose",
      recommendation = "recommendation",
	    receiveInfoJson = Some(receivedInfoJs)
    )

    val filePaths = request.body.files.map(_.ref.getAbsolutePath)

    (checkUpManager ? ModifyCheckUp(checkUp, filePaths)).mapTo[Int].map { _ =>
      Ok("OK")
    }.recover { case error =>
      logger.error(s"Error occurred during adding checkUp.", error)
      InternalServerError
    }
  }}

  def getCheckUpsByPatientId(patientId: Int) = Action.async { implicit request => asyncAuth {
    (checkUpManager ? GetCheckUpsByPatientId(patientId)).mapTo[Seq[CheckUp]].map { checkUps =>
      Ok(Json.toJson(checkUps))
    }.recover { case error =>
      logger.error(s"Error occurred during getting check ups. PatientId: $patientId", error)
      InternalServerError
    }
  }}

	def supervisedOut(patientId: Int) = Action.async(parse.json[SupervisedOut]) { implicit request => asyncAuth {
		(patientManager ? PatientSupervisedOut(patientId, request.body)).mapTo[Int].map { _ =>
			Ok("OK")
		}.recover { case error =>
			logger.error(s"Error occurred during changing supervise status.", error)
			InternalServerError
		}
	}}

  def uploadPatients = Action.async(parse.multipartFormData) { implicit request => asyncAuth {
//    val dataPart = request.body.asFormUrlEncoded
    request.body.file("file").map { filePart =>
      parseSpreadsheet(filePart.ref.path.toFile) match {
        case Right(rows) => createPatients(rows)
        case Left(failReason) => Future.successful(BadRequest(failReason))
      }
    }.getOrElse {
      Future { Ok("File topilmadi.") }
    }
  }}

  def createPatients(rows: List[List[String]]) = {
    (departmentManager ? GetAllVillages).mapTo[Seq[Village]].flatMap { villages =>
      try {
        val patients = validatePatients(rows, villages)
        (patientManager ? CreatePatients(patients)).mapTo[List[Int]].map { ids =>
          logger.info(s"All patients imported from file. Count: $ids")
          Ok("OK")
        }.recover { case error =>
          logger.error(s"Creating patients error", error)
          InternalServerError
        }
      } catch {
        case error: SpreadsheetException =>
          logger.warn(s"SpreadsheetException: ", error)
          Future { BadRequest(error.errorText) }
        case error: Throwable =>
          logger.error(s"Validate Patients from spreadsheet Error: ", error)
          Future { InternalServerError }
      }
    }
  }

  private def validatePatients(rows: List[List[String]], villages: Seq[Village]) = {
    val header = rows.head
    rows.tail.zipWithIndex.map { case (cyrillicRow, i) =>
      val index = i + 1
      val cells = cyrillicRow.map(cyril2Latin)
      logger.info(s"ROW $index | $cells")

      val throwError = (ind: Int) => throw SpreadsheetException(s"${header(ind)} ustunida xatolik aniqlandi. Qator raqami: $index")
      val getRequired = (ind: Int) =>
        if (cells(ind).isEmpty) {
          throwError(ind)
        } else {
          cells(ind)
        }

      val getOptional = (value: String) =>
        if (value.isEmpty) {
          None
        } else {
          Some(value)
        }

      val fullName = cells.head.trim.split(" ")
      val (firstName, lastName, middleName) = fullName.size match {
        case 4 => (fullName(1), fullName(0), fullName(2) + " " + fullName(3))
        case 3 => (fullName(1), fullName(0), fullName(2))
        case 2 => (fullName(1), fullName(0), "")
        case 1 => (fullName(1), "", "")
        case _ => throwError(0)
      }
      val gender = cells(1) match {
        case "e" => Gender.Male
        case "a" => Gender.Female
        case _ => throwError(1)
      }
      val birthDate = Try(parseDate(cells(2), "dd.MM.yyyy")).toOption match {
        case Some(validDate) => validDate
        case None => throwError(2)
      }
      val clientGroup = cells(11) match {
        case "I-kl.gr" => ClientGroup.I
        case "II-kl.gr" => ClientGroup.II
        case "III-kl.gr" => ClientGroup.III
        case "IV-kl.gr" => ClientGroup.IV
        case _ =>
          throwError(11)
      }
      logger.info(s"!!!$villages")
      val villageId = villages.find(_.name.contains(getRequired(3).take(5))) match {
        case Some(village) => village.id.get
        case None => throwError(3)
      }
      val supervisedOutJs = getOptional(cells(13)).map { supervisedOutDate =>
        val status = cells(12)
        Try(parseDate(supervisedOutDate, "dd.MM.yyyy")).toOption match {
          case Some(validDate) => Json.toJson(
            SupervisedOut(
              date = validDate,
              reason = status match {
                case "dead" => SupervisedOutReason.Dead
                case _ => SupervisedOutReason.Recovery
              },
              comments = getOptional(status)
            )
          )
          case None => throwError(13)
        }
      }
      val patientDataJs = Json.toJson(PatientData(
        province = getRequired(4),
        street = getOptional(cells(5)),
        home = getOptional(cells(6)),
        work = getOptional(cells(8)),
        position = None,
        bloodType = BloodType.I_+,
        phoneNumber = getOptional(cells(9))
      ))

      Patient(
        firstName = getOptional(firstName),
        lastName = getOptional(lastName),
        middleName = getOptional(middleName),
        passportId = getRequired(7),
        gender = gender,
        birthDate = birthDate,
        villageId = villageId,
        icd = getRequired(10),
        clientGroup = clientGroup,
        patientDataJson = Some(patientDataJs),
        supervisedOutJson = supervisedOutJs
      )
    }
  }

  private def parseDate(dateStr: String, format: String = "dd.MM.yyyy HH:mm") = {
    val dateFormat = new SimpleDateFormat(format)
    dateFormat.parse(dateStr)
  }

}