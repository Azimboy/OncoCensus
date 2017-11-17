package models

import java.util.Date

import org.joda.time.{DateTime, Hours}
import play.api.libs.json.Json

object UserAccountProtocol {

	private val UserUnlocksInHours = 12

	case class UserAccount(
		id: Option[Int] = None,
		login: String,
		passwordHash: String,
		firstName: Option[String] = None,
		lastName: Option[String] = None,
		roleCodes: Option[String] = None,
		createdAt: Option[Date] = None,
		updatedAt: Option[Date] = None,
		email: Option[String] = None,
		phoneNumber: Option[String] = None,
		expiresAt: Option[Date] = None,
		failedAttemptsCount: Int = 0,
		blockedAt: Option[Date] = None
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
	implicit val userAccountFormatter = Json.format[UserAccount]

	case class AddUserAccount(userAccount: UserAccount)

}
