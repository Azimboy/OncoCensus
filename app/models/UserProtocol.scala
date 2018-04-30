package models

import java.util.Date

import models.AppProtocol.Department
import org.joda.time.{DateTime, Hours}
import play.api.libs.json.Json

object UserProtocol {

	private val UserUnlocksInHours = 12

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
	case object BlockedUserAccount extends LoginAttemptsFailure
	case object LoginDoesNotMatch extends LoginAttemptsFailure
	case object RoleDoesNotMatch extends LoginAttemptsFailure
	case class WrongPassword(failedAttemptsCount: Int) extends LoginAttemptsFailure

	case class CheckUserLogin(login: String, password: String)

	case class ModifyUser(user: User)
	case object GetAllUsers
	sealed case class Role(code: String, name: String)

	object Administrator extends Role("administrator", "Administrator")
	object Role1 extends Role("role1", "Role 1")
	object Role2 extends Role("role2", "Role 2")

	val roles = Seq(Administrator, Role1, Role2)

	implicit val roleFormat = Json.format[Role]
	implicit val userFormat = Json.format[User]

}
