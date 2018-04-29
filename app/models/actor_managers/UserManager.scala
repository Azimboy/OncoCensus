package models.actor_managers

import java.util.Date

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import javax.inject.{Inject, Named, Singleton}
import models.UserProtocol.{GetAllUsers, ModifyUser, User}
import models.actor_managers.EncryptionManager.{DecryptUsers, EncryptUser}
import models.daos.UsersDao

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

}
