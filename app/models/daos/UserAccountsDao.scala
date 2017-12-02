package models.daos

import java.util.Date
import javax.inject.{Inject, Singleton}

import com.google.inject.ImplementedBy
import models.UserAccountProtocol._
import models.utils.Date2SqlDate
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

trait UserAccountsComponent extends DepartmentsComponent
  { self: HasDatabaseConfigProvider[JdbcProfile] =>

  import dbConfig.profile.api._

  class UserAccounts(tag: Tag) extends Table[UserAccount](tag, "user_accounts") with Date2SqlDate {
    val departments = TableQuery[Departments]

    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def createdAt = column[Date]("created_at")
    def loginEncr = column[String]("login_encr")
    def passwordHashEncr = column[String]("password_hash_encr")
    def firstNameEncr = column[String]("first_name_encr")
    def lastNameEncr = column[String]("last_name_encr")
    def middleNameEncr = column[String]("middle_name_encr")
    def departmentId = column[Int]("department_id")
    def roleCodesEncr = column[String]("role_codes_encr")
    def emailEncr = column[String]("email_encr")
    def phoneNumberEncr = column[String]("phone_number_encr")
    def updatedAt = column[Date]("updated_at")
    def expiresAt = column[Date]("expires_at")
    def failedAttemptsCount = column[Int]("failed_attempts_count")
    def blockedAt = column[Option[Date]]("blocked_at")

    def * = (id.?, createdAt.?, loginEncr, passwordHashEncr, firstNameEncr.?, lastNameEncr.?, middleNameEncr.?, departmentId.?, roleCodesEncr.?,
       emailEncr.?, phoneNumberEncr.?, updatedAt.?, expiresAt.?, failedAttemptsCount, blockedAt).shaped <>
      (t => {
        val fields =
          (t._1, t._2, t._3, t._4, t._5, t._6, t._7, t._8, t._9, t._10, t._11, t._12, t._13, t._14, t._15, None)
        (UserAccount.apply _).tupled(fields)
      },
        (i: UserAccount) =>
          UserAccount.unapply(i).map { t =>
            (t._1, t._2, t._3, t._4, t._5, t._6, t._7, t._8, t._9, t._10, t._11, t._12, t._13, t._14, t._15)
          }
      )

    def department = foreignKey("user_accounts_fk_department_id", departmentId, departments)(_.id)
  }
}

@ImplementedBy(classOf[UserAccountsImpl])
trait UserAccountsDao {
  def findById(id: Int): Future[Option[UserAccount]]
  def create(userAccount: UserAccount): Future[Int]
  def findAll: Future[Seq[UserAccount]]
  def updateUserAccount(userAccount: UserAccount): Future[Int]
  def updateUserPasswordHashEncr(userId: Int, passwordHashEncr: String): Future[Int]
  def updateUserPasswordHashEncrAndExpiresDate(userId: Int, passwordHashEncr: String, expiresAt: Date): Future[Int]
  def checkLoginUser(loginEncr: String, passwordHashEncr: String): Future[Option[UserAccount]]
  def checkPasswordWithCurrent(userId: Int, passwordHashEncr: String): Future[Option[UserAccount]]
  def updateUserAccountBlockStatusByLogin(loginEncr: String, blockedAt: Option[Date]): Future[Int]
}

@Singleton
class UserAccountsImpl @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
                                (implicit val ec: ExecutionContext)
  extends UserAccountsDao
  with UserAccountsComponent
  with HasDatabaseConfigProvider[JdbcProfile]
  with Date2SqlDate {

  import dbConfig.profile.api._

  val userAccounts = TableQuery[UserAccounts]
  val departments = TableQuery[Departments]

  override def findById(id: Int) = {
    db.run {
      userAccounts.filter(_.id === id).result.headOption
    }
  }

  override def create(userAccount: UserAccount) = {
    db.run {
      (userAccounts returning userAccounts.map(_.id)
        into ((r, id) => id)
        ) += userAccount
    }
  }

  override def findAll(): Future[Seq[UserAccount]] = {
    db.run {
      userAccounts.join(departments).on(_.departmentId === _.id).result
    }.map(_.map { case (userAccount, department) =>
      userAccount.copy(department = Some(department))
    })
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