package models.actor_managers

import java.util.Date

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import javax.inject.{Inject, Named, Singleton}
import models.UserProtocol._
import models.actor_managers.EncryptionManager.{DecryptUsers, EncryptUser}
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
		} yield decrUsers.map(_.copy(passwordHash = ""))
	}

	def checkUserLogin(login: String, password: String): Future[Either[LoginAttemptsFailure, User]] = {
		getDecrUserByLogin(login.toLowerCase, password)
//			.map {
//			case Right(decrUserAccount) =>
//				userAccountBaseWithFailure(
//					userRoleCodeSet.intersect(appRoleCodeSet).nonEmpty || userManagedAppCodeSet.contains(app.code),
//					decrUserAccount
//				)
//			case l @ Left(_) => Left(l.left.get)
//		}
	}

//	val userAccountBaseWithFailure = (f: Boolean, userAccount: UserAccount) => if (f) {
//		Right(createUserAccountBase(userAccount))
//	} else {
//		log.warning(s"Login failed [userAccountId=${userAccount.id}, ManagedApps=${userAccount.managedAppCodes}, RoleCodes=${userAccount.roleCodes}, reason: Role does not match]")
//		Left(RoleDoesNotMatch)
//	}

	private def getDecrUserByLogin(login: String, password: String): Future[Either[LoginAttemptsFailure, User]] = {
		(for {
			encrUsers <- usersDao.findAll
			decrUsers <- (encryptionManager ? DecryptUsers(encrUsers)).mapTo[Seq[User]]
		} yield decrUsers).map { users =>
			val passwordHash = createHash(password)
      log.info(s"$users")
			users.find { user =>
				user.login.toLowerCase == login.toLowerCase && user.passwordHash == passwordHash
			} match {
				case Some(user) => Right(user)
				case None => Left(WrongPassword(1))
			}
		}
	}

//	private def checkForFailedAttempts(users: Seq[User], login: String, password: String) = {
//		val passwordHash = createHash(password)
//		users.find(userFilter(login, passwordHash)).map { user =>
//			user.blockedAt match {
//				case Some(_) => checkBlockedUser(user, isValidUser = true)
//				case None =>
//					for {
//						loginEncr <- encrText(user.login)
//						_ <- usersDao.updateFailedAttemptsCountByLogin(loginEncr, 0)
//					} yield Right(user)
//			}
//		}.getOrElse {
//			val failedUser = users.find(_.login == login)
//			failedUser.map { fUser =>
//				log.warning(s"Login failed [failed User=${fUser.id}, reason: Wrong password]")
//				val failedAttemptsCount = fUser.failedAttemptsCount + 1
//				fUser.blockedAt match {
//					case Some(_) => checkBlockedUser(fUser, isValidUser = false)
//					case None =>
//						for {
//							loginEncr <- encrText(fUser.login)
//							clientCodeEncr <- encrText(fUser.clientCode.get)
//							_ <- userAccountsDao.updateFailedAttemptsCountByLogin(loginEncr, failedAttemptsCount, clientCodeEncr)
//						} yield Left(WrongPassword(failedAttemptsCount))
//				}
//			}.getOrElse {
//				log.warning(s"Login failed, reason: Login does not match]")
//				Future.successful(Left(LoginDoesNotMatch))
//			}
//		}
//	}

//	private def checkBlockedUser(user: UserAccount, isValidUser: Boolean): Future[Either[LoginAttemptsFailure, UserAccount]] = {
//		if (user.isBlocked) {
//			Future.successful(Left(BlockedUserAccount))
//		} else {
//			if (isValidUser) {
//				updateUserAccountBlockStatus(user.login, user.clientCode.get).map(_ => Right(user))
//			} else {
//				updateUserAccountBlockStatus(user.login, user.clientCode.get, None, Some(1)).map(_ => Left(WrongPassword(1)))
//			}
//		}
//	}

}
