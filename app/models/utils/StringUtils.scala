package models.utils

import java.security.MessageDigest

import com.typesafe.scalalogging.LazyLogging
import org.apache.commons.codec.binary.Base64

object StringUtils extends LazyLogging {

  private val emailRegex = """^[a-zA-Z0-9\.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$""".r

  def isValidEmail(email: String): Boolean = {
    if (email == null) {
      false
    } else {
      emailRegex.findFirstIn(email).nonEmpty
    }
  }

  def createHash(text: String): String = {
    val digest = MessageDigest.getInstance("SHA1")
    digest.digest(text.getBytes("UTF-8")).map(0xFF & _).map("%02x".format(_)).mkString
  }

  def bytesToHash(bytes: Array[Byte]): String = {
    val base64Str = Base64.encodeBase64String(bytes)
    createHash(base64Str)
  }

  def md5(s: String): String = {
    MessageDigest.getInstance("MD5").digest(s.getBytes).map("%02x".format(_)).mkString
  }

  def stringToList(str: String): List[String] = {
    str.split("[\\s,;]+").filter(_.nonEmpty).toList
  }

  def maskMiddlePart(str: String, charsLeft: Int, charsRight: Int, maskChar: String = "X"): String = {
    if (str.length > charsLeft + charsRight) {
      str.take(charsLeft) + maskChar * (str.length - charsLeft - charsRight) + str.takeRight(charsRight)
    } else {
      maskChar * str.length
    }
  }

  def base64UrlEncode(str: String) = {
    import java.util.Base64
    Base64.getUrlEncoder.withoutPadding.encodeToString(str.getBytes)
  }

  def base64UrlDecode(str: String): String = {
    import java.util.Base64
    if (!str.contains("//")) {
      val decodedBytes = Base64.getUrlDecoder.decode(str.getBytes)
      new String(decodedBytes)
    } else {
      str
    }
  }

}