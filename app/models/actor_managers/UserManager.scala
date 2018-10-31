package models.actor_managers

import java.util.Date

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import javax.inject.{Inject, Named, Singleton}
import models.AppProtocol.Department
import models.UserProtocol._
import models.actor_managers.EncryptionManager._
import models.daos.{DepartmentsDao, UsersDao}
import models.utils.StringUtils.createHash

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UserManager @Inject()(@Named("encryption-manager") encryptionManager: ActorRef,
                                   val usersDao: UsersDao,
																	 val departmentsDao: DepartmentsDao)
                                  (implicit val ec: ExecutionContext)
	extends Actor
		with ActorLogging {

	implicit val defaultTimeout = Timeout(60.seconds)

	override def receive: Receive = {
		case CreateAdmin =>
			createAdminIfNecessary().pipeTo(sender())

		case ModifyUser(user) =>
			modifyUser(user).pipeTo(sender())

		case GetAllUsers =>
			getAllUsers().pipeTo(sender())

		case CheckUserLogin(login, password) =>
			checkUserLogin(login, password).pipeTo(sender())

		case UpdateUsersBlockStatus(login, blockedAt) =>
			updateUsersBlockStatus(login, blockedAt).pipeTo(sender())

		case GetUserByLogin(login) =>
			getUserByLogin(login).pipeTo(sender())
	}

	def createAdminIfNecessary(): Future[Any] = {
		usersDao.usersCount().map { usersCount =>
			if (usersCount == 0) {
				createAdministration()
			}
		}
	}

	private def createAdministration() = {
		val department = Department(
			createdAt = Some(new Date),
			name = "Xonqa tuman ko'p tarmoqli poliklinikasi",
			districtId = 7
		)
		val admin = User(
			createdAt = Some(new Date),
			login = "admin",
			passwordHash = createHash("123"),
			firstName = Some("Adminbek"),
			lastName = Some("Adminov"),
			middleName = Some("Adminovich"),
			roleCodes = Some(Administrator.code)
		)
		(for {
			encrDepartment <- (encryptionManager ? EncryptDepartment(department)).mapTo[Department]
			encrAdmin <- (encryptionManager ? EncryptUser(admin)).mapTo[User]
			departmentId <- departmentsDao.create(encrDepartment)
			_ <- usersDao.create(encrAdmin.copy(departmentId = Some(departmentId)))
		} yield ()).map { _ =>
			log.info("Administrator successfully created!")
		}
	}

	def modifyUser(user: User): Future[Int] = {
		for {
			encryptedUser <- (encryptionManager ? EncryptUser(user)).mapTo[User]
			id <- user.id match {
				case Some(userId) =>
					log.info(s"Updating existing user. ID: $userId")
					usersDao.update(encryptedUser.copy(updatedAt = Some(new Date)))
				case None => usersDao.create(encryptedUser)
			}
		} yield id
	}

	def getAllUsers(): Future[Seq[User]] = {
		for {
			encrUsers <- usersDao.findAll
			decrUsers <- (encryptionManager ? DecryptUsers(encrUsers)).mapTo[Seq[User]]
		} yield decrUsers
	}

	def checkUserLogin(login: String, password: String): Future[Either[LoginAttemptsFailure, User]] = {
		for {
			encrLogin <- encrText(login)
			encrOptUser <- usersDao.findByLogin(encrLogin).mapTo[Option[User]]
			maybeUser <- (encryptionManager ? DecryptOptUser(encrOptUser)).mapTo[Option[User]]
			result <- maybeUser match {
				case Some(user) =>
					checkForFailedAttempts(user: User, createHash(password))
				case None =>
					log.warning(s"Login failed, reason: Login does not match]")
					Future.successful(Left(UserNotFound))
			}
		} yield result
	}

	def checkForFailedAttempts(user: User, passwordHash: String): Future[Either[LoginAttemptsFailure, User]] = {
		if (user.passwordHash == passwordHash) {
			user.blockedAt match {
				case Some(_) => checkBlockedUser(user, isValidUser = true)
				case None =>
					for {
						loginEncr <- encrText(user.login)
						_ <- usersDao.updateFailedAttemptsCountByLogin(loginEncr, 0)
					} yield Right(user)
			}
		} else {
			log.warning(s"Login failed [failed User=${user.id}, reason: Wrong password]")
			val failedAttemptsCount = user.failedAttemptsCount + 1
			user.blockedAt match {
				case Some(_) => checkBlockedUser(user, isValidUser = false)
				case None =>
					for {
						loginEncr <- encrText(user.login)
						_ <- usersDao.updateFailedAttemptsCountByLogin(loginEncr, failedAttemptsCount)
					} yield Left(WrongPassword(failedAttemptsCount))
			}
		}
	}

	private def checkBlockedUser(user: User, isValidUser: Boolean): Future[Either[LoginAttemptsFailure, User]] = {
		if (user.isBlocked) {
			Future.successful(Left(BlockedUser))
		} else {
			if (isValidUser) {
				updateUsersBlockStatus(user.login).map(_ => Right(user))
			} else {
				updateUsersBlockStatus(user.login, None, Some(1)).map(_ => Left(WrongPassword(1)))
			}
		}
	}

	def updateUsersBlockStatus(login: String, blockedAt: Option[Date] = None, failedAttemptsCount: Option[Int] = None): Future[Unit] = {
		(for {
			loginEncr <- encrText(login.toLowerCase)
			_ <- usersDao.updateUserBlockStatusByLogin(loginEncr, blockedAt)
			_ <- usersDao.updateFailedAttemptsCountByLogin(loginEncr, failedAttemptsCount.getOrElse(0))
		} yield ()).recover { case err =>
			log.error(err, s"Error occurred during updating block status user account")
		}
	}

	def decrText(encrText: String) = {
		(encryptionManager ? DecryptText(encrText)).mapTo[String]
	}

	def encrText(decrText: String) = {
		(encryptionManager ? EncryptText(decrText)).mapTo[String]
	}

	def getUserByLogin(login: String) = {
		for {
			encrLogin <- encrText(login)
			encrUserOpt <- usersDao.findByLogin(encrLogin).mapTo[Option[User]]
			maybeUser <- (encryptionManager ? DecryptOptUser(encrUserOpt)).mapTo[Option[User]]
		} yield maybeUser
	}

}
