package models

import java.util.Date

import models.AppProtocol.Department
import org.joda.time.{DateTime, Hours}
import play.api.libs.json.Json
import scala.concurrent.duration.DurationInt

object UserProtocol {

	private val UserUnlocksInHours = 12
	val FailedAttemptsCountForBlockUser = 5

	case class User(
		id: Option[Int] = None,
		createdAt: Option[Date] = None,
		login: String,
		passwordHash: String,
		firstName: Option[String] = None,
		lastName: Option[String] = None,
		middleName: Option[String] = None,
		departmentId: Option[Int] = None,
		roleCodes: Option[String] = None,
		email: Option[String] = None,
		phoneNumber: Option[String] = None,
		updatedAt: Option[Date] = None,
		expiresAt: Option[Date] = None,
		failedAttemptsCount: Int = 0,
		blockedAt: Option[Date] = None,
		department: Option[Department] = None
	) {
		def isBlocked = blockedAt match {
			case Some(blockedDate) =>
				val currentTime = new DateTime()
				val blockedTime = new DateTime(blockedDate)
				val hours = Hours.hoursBetween(blockedTime, currentTime).getHours
				hours <= UserUnlocksInHours
			case None => false
		}
	}

	sealed trait LoginAttemptsFailure
	case object BlockedUser extends LoginAttemptsFailure
	case object UserNotFound extends LoginAttemptsFailure
	case class WrongPassword(failedAttemptsCount: Int) extends LoginAttemptsFailure

	case class CheckUserLogin(login: String, password: String)
	case class UpdateUsersBlockStatus(login: String, blockedAt: Option[Date])

	case object CreateAdmin
	case class ModifyUser(user: User)
	case object GetAllUsers
	case class GetUserByLogin(login: String)

	sealed case class Role(code: String, name: String)

	object Administrator extends Role("administrator", "Administrator")
	object Potogistolog extends Role("potogistolog", "Potogistolog")

	val roles = Seq(Potogistolog)

	implicit val roleFormat = Json.format[Role]
	implicit val userFormat = Json.format[User]

  val userSessionKey = "onco.census.user"
	val roleSessionKey = s"$userSessionKey.role"
  val sessionDuration = 1.hour

}
