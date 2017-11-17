package models.daos

import java.util.Date
import javax.inject.{Inject, Singleton}

import models.UserAccountProtocol._
import com.google.inject.ImplementedBy
//import com.typesafe.scalalogging.LazyLogging
import models.utils.Date2SqlDate
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.Future

trait UserAccountsComponent { self: HasDatabaseConfigProvider[JdbcProfile] =>

  import dbConfig.profile.api._

  class UserAccounts(tag: Tag) extends Table[UserAccount](tag, "UserAccounts") with Date2SqlDate {

    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def loginEncr = column[String]("login_encr")
    def passwordHashEncr = column[String]("password_hash_encr")
    def firstNameEncr = column[String]("first_name_encr")
    def lastNameEncr = column[String]("last_name_encr")
    def roleCodesEncr = column[String]("role_codes_encr")
    def createdAt = column[Date]("created_at")
    def updatedAt = column[Date]("updated_at")
    def emailEncr = column[String]("email_encr")
    def phoneNumberEncr = column[String]("phone_number_encr")
    def expiresAt = column[Date]("expires_at")
    def failedAttemptsCount = column[Int]("failed_attempts_count")
    def blockedAt = column[Option[Date]]("blocked_at")

    def * = (id.?, loginEncr, passwordHashEncr, firstNameEncr.?, lastNameEncr.?, roleCodesEncr.?,
      createdAt.?, updatedAt.?, emailEncr.?, phoneNumberEncr.?, expiresAt.?, failedAttemptsCount, blockedAt) <> (UserAccount.tupled, UserAccount.unapply)

  }
}

@ImplementedBy(classOf[UserAccountsImpl])
trait UserAccountsDao {
  def findById(id: Int): Future[Option[UserAccount]]
  def create(userAccount: UserAccount): Future[Int]
  def updateUserAccount(userAccount: UserAccount): Future[Int]
  def updateUserPasswordHashEncr(userId: Int, passwordHashEncr: String): Future[Int]
  def updateUserPasswordHashEncrAndExpiresDate(userId: Int, passwordHashEncr: String, expiresAt: Date): Future[Int]
  def checkLoginUser(loginEncr: String, passwordHashEncr: String): Future[Option[UserAccount]]
  def checkPasswordWithCurrent(userId: Int, passwordHashEncr: String): Future[Option[UserAccount]]
  def updateUserAccountBlockStatusByLogin(loginEncr: String, blockedAt: Option[Date]): Future[Int]

}

@Singleton
class UserAccountsImpl @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
  extends UserAccountsDao
  with UserAccountsComponent
  with HasDatabaseConfigProvider[JdbcProfile]
  with Date2SqlDate {

  import dbConfig.profile.api._

  val userAccounts = TableQuery[UserAccounts]

  override def findById(id: Int) = {
    db.run {
      userAccounts.filter(_.id === id).result.headOption
    }
  }

  override def create(userAccount: UserAccount) = {
    db.run {
      (userAccounts returning userAccounts.map(_.id)
        into ((r,id) => id)
        ) += userAccount
    }
  }

  override def updateUserAccount(userAccount: UserAccount): Future[Int] = {
    db.run {
      userAccounts.filter(_.id === userAccount.id).update(userAccount)
    }
  }

  override def updateUserPasswordHashEncr(userId: Int, passwordHashEncr: String): Future[Int] = {
    db.run {
      userAccounts.filter(_.id === userId)
        .map(row => (row.passwordHashEncr, row.updatedAt))
        .update(passwordHashEncr, new Date)
    }
  }

  override def updateUserPasswordHashEncrAndExpiresDate(userId: Int, passwordHashEncr: String, expiresAt: Date): Future[Int] = {
    db.run {
      userAccounts.filter(_.id === userId)
        .map(row => (row.passwordHashEncr, row.updatedAt, row.expiresAt))
        .update(passwordHashEncr, new Date, expiresAt)
    }
  }

  override def checkLoginUser(loginEncr: String, passwordHashEncr: String) = {
    db.run {
      userAccounts.filter(user => user.loginEncr === loginEncr && user.passwordHashEncr === passwordHashEncr).result.headOption
    }
  }

  override def checkPasswordWithCurrent(userId: Int, passwordHashEncr: String): Future[Option[UserAccount]] = {
    db.run {
      userAccounts.filter(user => user.id === userId && user.passwordHashEncr === passwordHashEncr).result.headOption
    }
  }

  override def updateUserAccountBlockStatusByLogin(loginEncr: String, blockedAt: Option[Date]): Future[Int] = {
    db.run {
      userAccounts.filter(_.loginEncr === loginEncr)
        .map(_.blockedAt).update(blockedAt)
    }
  }

}