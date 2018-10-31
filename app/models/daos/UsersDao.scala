package models.daos

import java.util.Date

import com.google.inject.ImplementedBy
import javax.inject.{Inject, Singleton}
import models.UserProtocol._
import models.utils.Date2SqlDate
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.Future

trait UsersComponent
  extends DepartmentsComponent { self: HasDatabaseConfigProvider[JdbcProfile] =>
  import models.utils.PostgresDriver.api._

  class Users(tag: Tag) extends Table[User](tag, "users") with Date2SqlDate {
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
        (User.apply _).tupled(fields)
      },
        (i: User) =>
          User.unapply(i).map { t =>
            (t._1, t._2, t._3, t._4, t._5, t._6, t._7, t._8, t._9, t._10, t._11, t._12, t._13, t._14, t._15)
          }
      )

    def department = foreignKey("users_fk_department_id", departmentId, departments)(_.id)
  }
}

@ImplementedBy(classOf[UsersImpl])
trait UsersDao {
  def usersCount(): Future[Int]
  //  def findById(id: Int): Future[Option[User]]
  def findByLogin(loginEncr: String): Future[Option[User]]
  def create(user: User): Future[Int]
  def findAll(): Future[Seq[User]]
  def update(user: User): Future[Int]
  def updateUserPasswordHashEncr(userId: Int, passwordHashEncr: String): Future[Int]
  def updateUserPasswordHashEncrAndExpiresDate(userId: Int, passwordHashEncr: String, expiresAt: Date): Future[Int]
  def checkLoginUser(loginEncr: String, passwordHashEncr: String): Future[Option[User]]
  def checkPasswordWithCurrent(userId: Int, passwordHashEncr: String): Future[Option[User]]
  def updateUserBlockStatusByLogin(loginEncr: String, blockedAt: Option[Date]): Future[Int]
  def updateFailedAttemptsCountByLogin(loginEncr: String, failedAttemptsCount: Int): Future[Int]
}

@Singleton
class UsersImpl @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
  extends UsersDao
    with UsersComponent
    with HasDatabaseConfigProvider[JdbcProfile]
    with Date2SqlDate {

  import dbConfig.profile.api._

  import scala.concurrent.ExecutionContext.Implicits.global

  val users = TableQuery[Users]
  val departments = TableQuery[Departments]

  override def usersCount() = {
    db.run(users.size.result)
  }

  override def findByLogin(loginEncr: String) = {
    db.run {
      users.join(departments).on(_.departmentId === _.id)
        .filter(_._1.loginEncr === loginEncr).result.headOption
    }.map(_.map { case (user, department) =>
      user.copy(department = Some(department))
    })
  }

  override def create(user: User) = {
    db.run {
      (users returning users.map(_.id)
        into ((r, id) => id)
        ) += user
    }
  }

  override def findAll(): Future[Seq[User]] = {
    db.run {
      users.join(departments).on(_.departmentId === _.id).result
    }.map(_.map { case (user, department) =>
      user.copy(department = Some(department))
    })
  }

  override def update(user: User): Future[Int] = {
    db.run {
      users.filter(_.id === user.id).update(user)
    }
  }

  override def updateUserPasswordHashEncr(userId: Int, passwordHashEncr: String): Future[Int] = {
    db.run {
      users.filter(_.id === userId)
        .map(row => (row.passwordHashEncr, row.updatedAt))
        .update(passwordHashEncr, new Date)
    }
  }

  override def updateUserPasswordHashEncrAndExpiresDate(userId: Int, passwordHashEncr: String, expiresAt: Date): Future[Int] = {
    db.run {
      users.filter(_.id === userId)
        .map(row => (row.passwordHashEncr, row.updatedAt, row.expiresAt))
        .update(passwordHashEncr, new Date, expiresAt)
    }
  }

  override def checkLoginUser(loginEncr: String, passwordHashEncr: String) = {
    db.run {
      users.filter(user => user.loginEncr === loginEncr && user.passwordHashEncr === passwordHashEncr).result.headOption
    }
  }

  override def checkPasswordWithCurrent(userId: Int, passwordHashEncr: String): Future[Option[User]] = {
    db.run {
      users.filter(user => user.id === userId && user.passwordHashEncr === passwordHashEncr).result.headOption
    }
  }

  override def updateUserBlockStatusByLogin(loginEncr: String, blockedAt: Option[Date]): Future[Int] = {
    db.run {
      users.filter(_.loginEncr === loginEncr)
        .map(_.blockedAt).update(blockedAt)
    }
  }

  override def updateFailedAttemptsCountByLogin(loginEncr: String, failedAttemptsCount: Int): Future[Int] = {
    db.run {
      users.filter(u => u.loginEncr === loginEncr)
        .map(_.failedAttemptsCount).update(failedAttemptsCount)
    }
  }

}