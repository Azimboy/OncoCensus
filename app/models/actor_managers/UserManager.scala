package models.actor_managers

import java.util.Date

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import javax.inject.{Inject, Named, Singleton}
import models.UserProtocol._
import models.actor_managers.EncryptionManager.{DecryptText, DecryptUsers, EncryptText, EncryptUser}
import models.daos.UsersDao
import models.utils.StringUtils.createHash

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UserManager @Inject()(@Named("encryption-manager") encryptionManager: ActorRef,
                                   val usersDao: UsersDao)
                                  (implicit val ec: ExecutionContext)
	extends Actor
		with ActorLogging {

	implicit val defaultTimeout = Timeout(60.seconds)

	override def receive: Receive = {
		case ModifyUser(user) =>
			modifyUser(user).pipeTo(sender())

		case GetAllUsers =>
			getAllUsers().pipeTo(sender())

		case CheckUserLogin(login, password) =>
			checkUserLogin(login, password).pipeTo(sender())

		case UpdateUsersBlockStatus(login, blockedAt) =>
			updateUsersBlockStatus(login, blockedAt).pipeTo(sender())
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
		getAllUsers().flatMap(users => checkForFailedAttempts(users, login, createHash(password)))
	}

	def checkForFailedAttempts(users: Seq[User], login: String, passwordHash: String): Future[Either[LoginAttemptsFailure, User]] = {
		users.find(user => user.login.toLowerCase == login.toLowerCase && user.passwordHash == passwordHash).map { user =>
			user.blockedAt match {
				case Some(_) => checkBlockedUser(user, isValidUser = true)
				case None =>
					for {
						loginEncr <- encrText(user.login)
						_ <- usersDao.updateFailedAttemptsCountByLogin(loginEncr, 0)
					} yield Right(user)
			}
		}.getOrElse {
			val failedUser = users.find(_.login == login)
			failedUser.map { fUser =>
				log.warning(s"Login failed [failed User=${fUser.id}, reason: Wrong password]")
				val failedAttemptsCount = fUser.failedAttemptsCount + 1
				fUser.blockedAt match {
					case Some(_) => checkBlockedUser(fUser, isValidUser = false)
					case None =>
						for {
							loginEncr <- encrText(fUser.login)
							_ <- usersDao.updateFailedAttemptsCountByLogin(loginEncr, failedAttemptsCount)
						} yield Left(WrongPassword(failedAttemptsCount))
				}
			}.getOrElse {
				log.warning(s"Login failed, reason: Login does not match]")
				Future.successful(Left(UserNotFound))
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

}
