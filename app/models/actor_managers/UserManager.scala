package models.actor_managers

import javax.inject.{Inject, Named, Singleton}

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import models.UserProtocol.{AddUser, GetAllUsers, User}
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
		case AddUser(user) =>
			addUser(user).pipeTo(sender())

		case GetAllUsers =>
			getAllUsers().pipeTo(sender())
	}

	def addUser(newUser: User): Future[Int] = {
		for {
			encryptedUser <- (encryptionManager ? EncryptUser(newUser)).mapTo[User]
			id <- usersDao.create(encryptedUser)
		} yield id
	}

	def getAllUsers(): Future[Seq[User]] = {
		for {
			encrUsers <- usersDao.findAll
			decrUsers <- (encryptionManager ? DecryptUsers(encrUsers)).mapTo[Seq[User]]
		} yield decrUsers
	}

}
