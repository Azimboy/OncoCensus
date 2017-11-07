package controllers
/*
import java.text.SimpleDateFormat
import java.util.Date
import javax.inject._

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.stream.scaladsl.Source
import cielo.assist.app.AppProtocol.{Message, MessageDeliveryFailure, SendingResult}
import cielo.assist.app.UserAccountProtocol._
import cielo.assist.util.CieloFileUtil
import cielo.assist.util.CieloFileUtil.ExcelFileParam
import cielo.assist.web.CieloConfigUtil._
import cielo.assist.web.JsonFormatUtils._
import cielo.assist.web.{ErrorHandlerWithStatus, SimpleAuth, TdIpWhitelist}
import com.github.tototoshi.csv.CSVWriter
import play.api.Configuration
import play.api.i18n.I18nSupport
import play.api.libs.iteratee.Enumerator
import play.api.libs.iteratee.streams.IterateeStreams
import play.api.libs.json.Json
import play.api.mvc.{BaseController, ControllerComponents}
import views.html.cielo_admin.super_manager._

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SuperManagerController @Inject() (val controllerComponents: ControllerComponents,
                                        val configuration: Configuration,
                                        indexTemplate: index,
                                        loginTemplate: login,
                                        implicit val actorSystem: ActorSystem
                                       )(implicit val ec: ExecutionContext)
    extends BaseController
    with I18nSupport
    with SimpleAuth
    with TdIpWhitelist
    with ErrorHandlerWithStatus
{

  implicit val currentConfig = getWebServerConfig(configuration)
  val userManager = getActorSelFromConfig("user-manager")
  val cieloAccountManager = getActorSelFromConfig("cielo-account-manager")

  private val LoginSessionKey = "cielo.super.manager.login"
  val SessionDuration = Some(10.minutes)

  implicit val showNotWhitelistedPage: Boolean = true

  def index = Action.async { implicit request => checkTdIp {
    val result = authBy(LoginSessionKey, SessionDuration) {
     Ok(indexTemplate())
    }

    if (result.header.status == UNAUTHORIZED) {
      Ok(loginTemplate())
    } else {
      result
    }
  }}

  def login = Action.async { implicit request => checkTdIp {
    Ok(loginTemplate())
  }}

  def loginPost = Action.async { implicit request => checkTdIp {
    val redirectWithError = Redirect(routes.SuperManagerController.index()).flashing("error" -> "loginFailed")
    loginPlayForm.bindFromRequest.fold(
      errorForm => {
        logger.info(s"errorForm: $errorForm")
        Future.successful(redirectWithError)
      },
      {
        case LoginForm(login, password) =>
          checkLogin(login, password).mapTo[Either[LoginAttemptsFailure, UserAccountBase]].map {
            case Right(user) =>
              Redirect(routes.SuperManagerController.index()).addingToSession(
                authInit(LoginSessionKey, login, SessionDuration) : _*
              )
            case _ => redirectWithError
          }.recover {
            case error =>
              logger.error("Error occurred during login to the super manager", error)
              redirectWithError
          }
      })
  }}

  private def checkLogin(login: String, password: String) = {
    userManager ? CheckLoginSuperManager(login, password, SuperManagerApp)
  }

  def logout = Action.async { implicit request => checkTdIp {
    Redirect(routes.SuperManagerController.index()).withSession(
      authClear(LoginSessionKey)
    )
  }}

  def usersList(clientCode: String) = Action.async { implicit request => authByAsync(LoginSessionKey) {
    (userManager ? GetUsersByClientCode(clientCode)).mapTo[Seq[UserAccountBase]].map { managers =>
      Ok(Json.toJson(managers))
    }
  }}

  def deletedUsersList(clientCode: String) = Action.async { implicit request => authByAsync(LoginSessionKey) {
    (userManager ? GetDeletedUsersByClientCode(clientCode)).mapTo[Seq[UserAccountBase]].map { managers =>
      Ok(Json.toJson(managers))
    }
  }}

  def internalUsersList(clientCode: String) = Action.async { implicit request => authByAsync(LoginSessionKey) {
    (userManager ? GetInternalUsersByClientCode(clientCode)).mapTo[Seq[UserAccountBase]].map { managers =>
      Ok(Json.toJson(managers))
    }
  }}

  private def getUserAccountBase(user: UserAccountWeb) = {
    // Super manager can't create Super manager
    require(!user.managedAppCodes.contains(SuperManagerApp.code))

    val specPartJs = Json.toJson(UserAccountSpecPart(
      costCenter = user.costCenter,
      costCenterManager = user.costCenterManager,
      department = user.department,
      isInternal = user.isInternal
    ))

    UserAccountBase(
      id = user.id,
      login = user.login,
      roleCodes = user.roleCodes,
      firstName = user.firstName,
      lastName = user.lastName,
      externalId = user.externalId,
      managedAppCodes = user.managedAppCodes,
      clientCode = user.clientCode,
      createdAt = Some(new Date),
      updatedAt = Some(new Date),
      email = user.email,
      phoneNumber = user.phoneNumber,
      specPart = Some(Json.stringify(specPartJs)),
      expiresAt = user.expiresAt,
      failedAttemptsCount = user.failedAttemptsCount,
      blockedAt = user.blockedAt,
      isFirstLogin = user.isFirstLogin
    )
  }

  def addUserManager() = Action.async(parse.json[UserAccountWeb]) { implicit request => authByAsync(LoginSessionKey) {
    val user = request.body
    val userAccountBase = getUserAccountBase(user)

    (userManager ? CheckIsLoginUnique(user.login)).mapTo[Boolean].flatMap {
      case true =>
        (userManager ? AddUserAccountBase(userAccountBase)).mapTo[Either[MessageDeliveryFailure, Int]].map {
          case Right(id) =>
            Ok(Json.toJson(id))
          case Left(err) =>
            Ok("Email sending containing the password was failed.")
        }.recover(handleErrorWithStatus("Error while adding user", "Error occurred. Please try again."))
      case false =>
        Ok(s"Login [${user.login}] already exist. Please choose another one.")
     }
  }}

  def updateUserManager(id: Int) = Action.async(parse.json[UserAccountWeb]) { implicit request => authByAsync(LoginSessionKey) {
    val webUser = request.body

    def updateUserAccountBase(user: UserAccountBase) = {
      (userManager ? UpdateUserAccountBase(getUserAccountBase(webUser))).mapTo[Int].map { _ =>
        Ok(Json.toJson("OK"))
      }.recover {
        case ex: Exception if ex.getMessage.contains("unique") =>
          Ok(s"Login already exist. Please choose another one.")
        case ex =>
          logger.error(s"Error while updating user ${user.id}", ex)
          Ok("Error occurred. Please try again.")
      }
    }

    (userManager ? GetUserAccount(id)).mapTo[UserAccountBase].flatMap { user =>
      val hasSameLogin = user.login.toLowerCase == webUser.login.toLowerCase()
      if (!hasSameLogin) {
        (userManager ? CheckIsLoginUnique(webUser.login)).mapTo[Boolean].flatMap {
          case true =>
            updateUserAccountBase(user)
          case false =>
            Ok(s"Login [${webUser.login}] already exist. Please choose another one.")
        }
      } else {
        updateUserAccountBase(user)
      }
    }
  }}

  def changePassword(id: Int) = Action.async(parse.json[UserPassword]) { implicit request => authByAsync(LoginSessionKey) {
    val password = request.body.password

    (userManager ? UpdateUserPassword(id, password)).mapTo[Int].map { _ =>
      Ok(Json.toJson("OK"))
    }
  }}

  def deleteUserManager(id: Int) = Action.async { implicit request => authByAsync(LoginSessionKey) {
    (userManager ? DeleteUserAccount(id)).mapTo[Int].map { _ =>
      Ok(Json.toJson("OK"))
    }
  }}

  def availableApps(clientCode: String) = Action.async { implicit  request => authByAsync(LoginSessionKey) {
    val apps = getAppsByClientCode(clientCode)
    Future.successful(Ok(Json.toJson(apps)))
  }}

  def availableRoles(clientCode: String) = Action.async { implicit  request => authByAsync(LoginSessionKey) {
    val roles = getRolesByClientCode(clientCode)
    Future.successful(Ok(Json.toJson(roles)))
  }}

  def clients = Action.async { implicit  request => authByAsync(LoginSessionKey) {
    val clients = getClientsList
    Future.successful(Ok(Json.toJson(clients)))
  }}

  def csvReport(clientCode: String) = Action.async { implicit request => authByAsync(LoginSessionKey) {
    (userManager ? GetUsersByClientCode(clientCode)).mapTo[Seq[UserAccountBase]].map { users =>
      getCsvReport(users, isForDeletedUsers = false)
    }
  }}

  def unblockUser(userId: Int) = Action.async { implicit request => authByAsync(LoginSessionKey) {
    (userManager ? UnblockAndGetUser(userId)).mapTo[UserAccountBase].flatMap { user =>
      if (user.email.isDefined) {
        val message = Message(agent = Some(""), email = user.email)
        val forgotPassword = ForgotPassword(user.id.get, message)
        (cieloAccountManager ? forgotPassword).mapTo[Either[MessageDeliveryFailure, String]].map {
          case Right(_) =>
            Ok(Json.toJson(SendingResult(isSent = true)))
          case Left(err) =>
            logger.error(s"Reset password email was not sent to user", err)
            Ok(Json.toJson(SendingResult(isSent = false, failReason = Some("Reset password email was not sent."))))
        }
      } else {
        Future.successful(Ok(Json.toJson(SendingResult(isSent = false, failReason = Some("This user hasn't got an email")))))
      }
    }.recover(handleInternalServerError("Error while sending forgot password email from super manager", "Error while sending forgot password email"))
  }}

  // Attestation Report
  def getExcelReport = Action.async { implicit request => authByAsync(LoginSessionKey) {
    val formParams = request.body.asFormUrlEncoded.get
    val clientCode = formParams("clientCode").headOption.getOrElse("")
    val managedAppCodesOptStr = formParams("managedAppCodes").headOption
    (userManager ? GetUsersByClientCode(clientCode)).mapTo[Seq[UserAccountBase]].map { users =>
      if (managedAppCodesOptStr.isDefined && managedAppCodesOptStr.forall(_.nonEmpty)) {
        val managedAppCodes = managedAppCodesOptStr.get.split(",").toSet
        val roleCodes = managedAppCodes.flatMap(appCode => getRolesByAppCode(appCode).map(_.code))
        val selectedUsers = users.filter { user =>
          roleCodes.intersect(user.roleCodes.getOrElse("").split(",").toSet).nonEmpty ||
          managedAppCodes.intersect(user.managedAppCodes.getOrElse("").split(",").toSet).nonEmpty
        }
        createExcelReport(selectedUsers)
      } else {
        createExcelReport(users)
      }
    }
  }}

  private def createExcelReport(users: Seq[UserAccountBase]) = {
    val dateFormat = new SimpleDateFormat("dd-MMM-yyyy")
    val dropdownValues = Map(
      "attestation" -> Seq("YES Valid User", "NO Please Remove", "Known but not needed")
    )
    val rowValues = users.map { user =>
      val (department, costCenter, costCenterManager) = user.specPart.map { specPart =>
        val sp = Json.parse(specPart).validate[UserAccountSpecPart].get
        (sp.department.getOrElse(""), sp.costCenter.getOrElse(""), sp.costCenterManager.getOrElse(""))
      }.getOrElse("", "", "")
      val acf2IdAndLogin = if (user.externalId.exists(_.equalsIgnoreCase(user.login))) {
        user.externalId.get
      } else {
        user.externalId.map { acf2id =>
          s"$acf2id\n${user.login}"
        }.getOrElse(user.login)
      }

      Seq(
        user.firstName.getOrElse(""),
        user.lastName.getOrElse(""),
        acf2IdAndLogin,
        getAppNamesWithRoleNamesForAttestationReport(user.managedAppCodes.getOrElse(""), user.roleCodes.getOrElse("")),
        user.createdAt.map(dateFormat.format).getOrElse(""),
        user.updatedAt.map(dateFormat.format).getOrElse(""),
        user.email.getOrElse(""),
        user.phoneNumber.getOrElse(""),
        department,
        costCenter,
        costCenterManager,
        "DROPDOWN:attestation"
      )
    }
    val excelBytes = CieloFileUtil.getUserManagementExcelBytes(ExcelFileParam(
      sheetName = "Known Users",
      columnTitles = Seq("FIRST NAME", "LAST NAME", "ACF2 ID\nCIELO LOGIN", "APPLICATION and ROLES",
        "CREATED ON", "UPDATED ON", "MAIL", "SMS/MOBILE", "DEPARTMENT", "COST CENTER",
        "COST CENTER\nOWNER", "ATTESTATION\nKnown User? YES/NO"),
      rowValues = rowValues,
      dropdownValues = dropdownValues
    ))
    val source = Source.fromPublisher(IterateeStreams.enumeratorToPublisher(Enumerator(excelBytes)))
    Ok.chunked(source).as("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
        .withHeaders(CONTENT_DISPOSITION -> s"attachment; filename=Attestation_Report.xlsx")
  }

  def deletedUsersCsvReport(clientCode: String) = Action.async { implicit request => authByAsync(LoginSessionKey) {
    (userManager ? GetDeletedUsersByClientCode(clientCode)).mapTo[Seq[UserAccountBase]].map { users =>
      getCsvReport(users, isForDeletedUsers = true)
    }
  }}

  def internalUsersCsvReport(clientCode: String) = Action.async { implicit request => authByAsync(LoginSessionKey) {
    (userManager ? GetInternalUsersByClientCode(clientCode)).mapTo[Seq[UserAccountBase]].map { users =>
      getCsvReport(users, isForDeletedUsers = false)
    }
  }}

  private def getAppNamesWithRoleNamesForAttestationReport(managedAppCodes: String, roleCodesStr: String) = {
    if (roleCodesStr.nonEmpty) {
      val roles = getRolesByRoleCodes(roleCodesStr.split(","))
      val appNameToRolesGroup = roles.groupBy(_.app.name)
      appNameToRolesGroup.map { appNameToRolesT =>
        val appName = appNameToRolesT._1
        val roleNames = appNameToRolesT._2.map(_.name)
        val rNames = if (roleNames.length == 1) {
            roleNames
          } else {
            roleNames.map(s => s"\n- $s")
          }
          s"$appName: ${rNames.mkString}"
      }.mkString("\n\n")
    } else {
      getAppNamesByAppCodes(managedAppCodes.split(",").toSeq).map { app =>
        s"$app: Manager"
      }.mkString("\n")
    }
  }

  private def getAppNamesWithRoleNames(roleCodesStr: String) = {
    val roles = getRolesByRoleCodes(roleCodesStr.split(","))
    val appNameToRolesGroup = roles.groupBy(_.app.name)
    appNameToRolesGroup.map { appNameToRolesT =>
      appNameToRolesT._2.map(_.name).mkString(", ") + " in " + appNameToRolesT._1
    }.mkString(" | ")
  }

  private def getAppNames(appCodesStr: String) = {
    val appNames = getAppNamesByAppCodes(appCodesStr.split(","))
    appNames.mkString(" | ")
  }

  private def getCsvReport(users: Seq[UserAccountBase], isForDeletedUsers: Boolean) = {
    val enumerator = Enumerator.outputStream { outputStream =>
      val writer = CSVWriter.open(outputStream)
      val dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm:ss")

      writer.writeRow(List(
        "Created At",
        "Updated At",
        "Login",
        "Roles",
        "Managed Apps",
        "First Name",
        "Last Name",
        "Email",
        "Phone Number",
        "Department",
        "ACF2 ID",
        "Cost Center",
        "Cost Center Manager's Name"
      ))

      for (user <- users) {
        var department = ""
        var costCenter = ""
        var costCenterManager = ""
        val userLogin = user.login
        val login = if (isForDeletedUsers) userLogin.split("_DELETED_")(0) else userLogin

        if (user.specPart.isDefined) {
          val specPart = Json.parse(user.specPart.get).validate[UserAccountSpecPart].get

          department = specPart.department.getOrElse("")
          costCenter = specPart.costCenter.getOrElse("")
          costCenterManager = specPart.costCenterManager.getOrElse("")
        }
        val appNames = getAppNames(user.managedAppCodes.getOrElse(""))
        writer.writeRow(List(
          user.createdAt.map(dateFormat.format).getOrElse(""),
          user.updatedAt.map(dateFormat.format).getOrElse(""),
          login,
          user.roleCodes.map(getAppNamesWithRoleNames).getOrElse(""),
          appNames,
          user.firstName.getOrElse(""),
          user.lastName.getOrElse(""),
          user.email.getOrElse(""),
          user.phoneNumber.getOrElse(""),
          department,
          user.externalId.getOrElse(""),
          costCenter,
          costCenterManager
        ))
      }

      writer.close()
    }
    val fileName = if (isForDeletedUsers) "Deleted_users_report.csv" else "Users_report.csv"

    val source = Source.fromPublisher(IterateeStreams.enumeratorToPublisher(enumerator))
    Ok.chunked(source).as("text/csv")
        .withHeaders(CONTENT_DISPOSITION -> s"attachment; filename=$fileName")
  }

}
*/