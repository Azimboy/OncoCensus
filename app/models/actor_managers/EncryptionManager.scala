package models.actor_managers

import java.io.FileInputStream
import java.security.KeyStore
import javax.crypto.spec.{IvParameterSpec, SecretKeySpec}
import javax.crypto.{BadPaddingException, Cipher}
import javax.inject.Inject

import akka.actor._
import akka.util.Timeout
import com.google.common.io.BaseEncoding
import models.AppProtocol.Department
import models.PatientProtocol.Patient
import models.UserAccountProtocol.UserAccount
import play.api.Configuration
import play.api.libs.json._

import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.util.Try

object EncryptionManager {
	case class EncryptText(text: String)
	case class EncryptTexts(texts: Seq[String])
	case class EncryptBytes(bytes: Array[Byte])
	case class DecryptText(text: String)
	case class DecryptTexts(texts: Seq[String])
	case class DecryptBytes(bytes: Array[Byte])

	case class EncryptUserAccount(userAccount: UserAccount)
	case class DecryptUserAccount(userAccount: UserAccount)
	case class DecryptUserAccounts(userAccounts: Seq[UserAccount])

	case class EncryptDepartment(userAccount: Department)
	case class DecryptDepartments(departments: Seq[Department])

	case class EncryptPatient(patient: Patient)
	case class DecryptPatient(patient: Patient)
	case class DecryptPatients(patients: Seq[Patient])
}

class EncryptionManager @Inject() (configuration: Configuration)
	extends Actor with Stash with ActorLogging
{

	val config = configuration.get[Configuration]("web-server")
	val keystorePath = config.get[String]("encr-manager.keystore-path")

	import EncryptionManager._
	import context.dispatcher

	case object InitStart
	case object InitDone

	implicit val defaultTimeout = Timeout(60.seconds)
	val RetryInitTimeout: FiniteDuration = 10.seconds

	val KeyAlias = "cielo-assist-aes"
	val InitialVectorBytes = Array[Byte](
		0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
		0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F
	)

	var cipherEncr: Cipher = _
	var cipherDecr: Cipher = _

	override def preStart() = {
		self ! InitStart
	}

	override def receive: Receive = {
		case InitStart =>
			Try {
				openKeystore("KEYSTORE-PASSWORD", "KEY-PASSWORD")
			}.map(_ => self ! InitDone).recover { case error =>
				log.warning(s"Keystore couldn't be opened. $error")
				context.system.scheduler.scheduleOnce(RetryInitTimeout, self, InitStart)
			}

		case InitDone =>
			unstashAll()
			context.become(receiveAfterInit)
			log.info(s"InitDone")

		case m =>
			stash()
	}

	def receiveAfterInit: Receive = {

		case EncryptText(text) =>
			sender() ! encryptText(text)

		case EncryptTexts(texts) =>
			sender() ! encryptTexts(texts)

		case EncryptBytes(bytes) =>
			sender() ! encryptBytes(bytes)

		case DecryptText(text) =>
			sender() ! decryptText(text)

		case DecryptTexts(texts) =>
			sender() ! decryptTexts(texts)

		case DecryptBytes(bytes) =>
			sender() ! decryptBytes(bytes)

		case EncryptUserAccount(userAccount) =>
			sender() ! encryptUserAccount(userAccount)

		case DecryptUserAccount(userAccount) =>
			sender() ! decryptUserAccount(userAccount)

		case DecryptUserAccounts(userAccounts) =>
			sender() ! decryptUserAccounts(userAccounts)

		case EncryptDepartment(department) =>
			sender() ! encryptDepartment(department)

		case DecryptDepartments(departments) =>
			sender() ! decryptDepartments(departments)

		case EncryptPatient(patient) =>
			sender() ! encryptPatient(patient)

		case DecryptPatient(patient) =>
			sender() ! decryptPatient(patient)

		case DecryptPatients(patients) =>
			sender() ! decryptPatients(patients)

	}

	private def openKeystore(keystorePassword: String, keyPassword: String): Unit = {
		val in = new FileInputStream(keystorePath)
		val keystore = KeyStore.getInstance("JCEKS")
		keystore.load(in, keystorePassword.toCharArray)

		val key = keystore.getKey(KeyAlias, keyPassword.toCharArray)
		val secretKeySpec = new SecretKeySpec(key.getEncoded, "AES")
		val ivSpec = new IvParameterSpec(InitialVectorBytes)

		val algo = "AES/CBC/PKCS5Padding"
		cipherEncr = Cipher.getInstance(algo)
		cipherEncr.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivSpec)
		cipherDecr = Cipher.getInstance(algo)
		cipherDecr.init(Cipher.DECRYPT_MODE, secretKeySpec, ivSpec)
	}

	var isEncrypting = false

	implicit val readsMap: Reads[Map[String, Any]] = Reads[Map[String, Any]](m => Reads.mapReads[Any](metaValueReader).reads(m))
	implicit val writesMap: Writes[Map[String, Any]] = Writes[Map[String, Any]](m => Writes.mapWrites[Any](metaValueWriter).writes(m))

	def metaValueReader(jsValue: JsValue): JsResult[Any] = jsValue match {
		case JsObject(m) => JsSuccess(m.map { case (k, v) => k -> metaValueReader(v) })
		case JsArray(arr) => JsSuccess(arr.map(metaValueReader))
		case JsBoolean(b) => JsSuccess(b).map(getEncrValue)
		case JsNumber(n) => JsSuccess(n).map(getEncrValue)
		case JsString(s) => JsSuccess(s).map(getEncrValue)
		case JsNull => JsSuccess("").map(getEncrValue)
		case badValue => JsError(s"$badValue is not a valid value")
	}

	def metaValueWriter(value: Any): JsValue = value match {
		case jsRes: JsSuccess[_] => metaValueWriter(jsRes.get)
		case m: Map[_, _] => JsObject(m.asInstanceOf[Map[String, Any]].map { case (k, v) => k -> metaValueWriter(v) })
		case arr: Seq[_] => JsArray(arr.map(metaValueWriter))
		case s: String => JsString(getDecrValue(s))
	}

	def getEncrValue(value: Any) = {
		if (isEncrypting) {
			encryptText(value.toString)
		} else {
			value
		}
	}

	def getDecrValue(value: String) = {
		if (isEncrypting) {
			value
		} else {
			decryptText(value)
		}
	}

	def encryptSpecPartJson(specPartJson: JsValue) = {
		isEncrypting = true
		val encrSpecPart = specPartJson.validate[Map[String, Any]].get
		Json.toJson(encrSpecPart)
	}

	def decryptSpecPartJson(specPartJson: JsValue) = {
		isEncrypting = false
		val decrSpecPart = specPartJson.validate[Map[String, Any]].get
		Json.toJson(decrSpecPart)
	}

	private def encryptText(text: String): String = {
		BaseEncoding.base64().encode(cipherEncr.doFinal(text.getBytes("UTF-8")))
	}

	def decryptText(text: String): String = {
		try {
			new String(cipherDecr.doFinal(BaseEncoding.base64().decode(text)))
		} catch {
			case e: BadPaddingException =>
				log.error(e, s"Couldn't decrypt text")
				throw e
		}
	}

	private def encryptTexts(texts: Seq[String]): Seq[String] = {
		texts.map(encryptText)
	}

	private def decryptTexts(texts: Seq[String]): Seq[String] = {
		texts.map(decryptText)
	}

	def encryptBytes(bytes: Array[Byte]): Array[Byte] = {
		cipherEncr.doFinal(bytes)
	}

	def decryptBytes(bytes: Array[Byte]): Array[Byte] = {
		cipherDecr.doFinal(bytes)
	}

	def encryptUserAccount(userAccount: UserAccount): UserAccount = {
		userAccount.copy(
			login = encryptText(userAccount.login),
			passwordHash = encryptText(userAccount.passwordHash),
			roleCodes = userAccount.roleCodes.map(encryptText),
			firstName = userAccount.firstName.map(encryptText),
			lastName = userAccount.lastName.map(encryptText),
			middleName = userAccount.middleName.map(encryptText),
			email = userAccount.email.map(encryptText),
			phoneNumber = userAccount.phoneNumber.map(encryptText)
		)
	}

	def decryptUserAccount(userAccount: UserAccount): UserAccount = {
		userAccount.copy(
			login = decryptText(userAccount.login),
			passwordHash = decryptText(userAccount.passwordHash),
			roleCodes = userAccount.roleCodes.map(decryptText),
			firstName = userAccount.firstName.map(decryptText),
			lastName = userAccount.lastName.map(decryptText),
			middleName = userAccount.middleName.map(decryptText),
			email = userAccount.email.map(decryptText),
			phoneNumber = userAccount.phoneNumber.map(decryptText),
			department = userAccount.department.map(dep => dep.copy(name = decryptText(dep.name)))
		)
	}

	def decryptUserAccounts(accounts: Seq[UserAccount]) = {
		accounts.map(decryptUserAccount)
	}

	def encryptDepartment(department: Department): Department = {
		department.copy(
			name = encryptText(department.name)
		)
	}

	def decryptDepartments(departments: Seq[Department]) = {
		departments.map(department => department.copy(name = decryptText(department.name)))
	}

	def encryptPatient(patient: Patient): Patient = {
		patient.copy(
			firstName = patient.firstName.map(encryptText),
			lastName = patient.lastName.map(encryptText),
			middleName = patient.middleName.map(encryptText),
			email = patient.email.map(encryptText),
			phoneNumber = patient.phoneNumber.map(encryptText),
			patientDataJson = patient.patientDataJson.map(encryptSpecPartJson)
		)
	}

	def decryptPatient(patient: Patient): Patient = {
		patient.copy(
			firstName = patient.firstName.map(decryptText),
			lastName = patient.lastName.map(decryptText),
			middleName = patient.middleName.map(decryptText),
			email = patient.email.map(decryptText),
			phoneNumber = patient.phoneNumber.map(decryptText),
			patientDataJson = patient.patientDataJson.map(decryptSpecPartJson)
		)
	}

	def decryptPatients(patients: Seq[Patient]): Any = {
		patients.map(decryptPatient)
	}

}