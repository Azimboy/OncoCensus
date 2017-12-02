package models

import java.util.Date

import models.AppProtocol.Department
import org.joda.time.{DateTime, Hours}
import play.api.libs.json.Json

object UserAccountProtocol {

	private val UserUnlocksInHours = 12

	case class UserAccount(
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
	implicit val userAccountFormat = Json.format[UserAccount]

	case class AddUserAccount(userAccount: UserAccount)
	case object GetAllUserAccounts

}
