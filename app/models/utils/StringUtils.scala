package models.utils

import java.security.MessageDigest

import com.typesafe.scalalogging.LazyLogging
import org.apache.commons.codec.binary.Base64

object StringUtils extends LazyLogging {

  private val emailRegex = """^[a-zA-Z0-9\.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$""".r

  val Cyril2Latin = Map(
    'А' -> "A",
    'Б' -> "B",
    'В' -> "V",
    'Г' -> "G",
    'Д' -> "D",
    'Е' -> "E",
    'Ё' -> "Yo",
    'Ж' -> "J",
    'З' -> "Z",
    'И' -> "I",
    'Й' -> "Y",
    'К' -> "K",
    'Л' -> "L",
    'М' -> "M",
    'Н' -> "N",
    'О' -> "O",
    'П' -> "P",
    'Р' -> "R",
    'С' -> "S",
    'Т' -> "T",
    'У' -> "U",
    'Ф' -> "F",
    'Х' -> "X",
    'Ц' -> "S",
    'Ч' -> "Ch",
    'Ш' -> "Sh",
    'Ў' -> "O‘",
    'Ь' -> "",
    'Қ' -> "Q",
    'Ъ' -> "'",
    'Э' -> "E",
    'Ю' -> "Yu",
    'Я' -> "Ya",
    'Ҳ' -> "H",
    'Ғ' -> "G‘",
    'а' -> "a",
    'б' -> "b",
    'в' -> "v",
    'г' -> "g",
    'д' -> "d",
    'е' -> "e",
    'ё' -> "yo",
    'ж' -> "j",
    'з' -> "z",
    'и' -> "i",
    'й' -> "y",
    'к' -> "k",
    'л' -> "l",
    'м' -> "m",
    'н' -> "n",
    'о' -> "o",
    'п' -> "p",
    'р' -> "r",
    'с' -> "s",
    'т' -> "t",
    'у' -> "u",
    'ф' -> "f",
    'х' -> "x",
    'ц' -> "s",
    'ч' -> "ch",
    'ш' -> "sh",
    'ў' -> "o‘",
    'ь' -> "",
    'қ' -> "q",
    'ъ' -> "'",
    'э' -> "e",
    'ю' -> "yu",
    'я' -> "ya",
    'ҳ' -> "h",
    'ғ' -> "g‘",
  )

  def cyril2Latin(kril: String): String = {
    val replaceYe = (c: Char) => c match {
      case 'Е' => "Ye"
      case 'е' => "ye"
      case _ => Cyril2Latin.getOrElse(c, c)
    }

    kril.zipWithIndex.map { case (c, i) =>
      if (i == 0 || kril(i - 1) == ' ') {
        replaceYe(c)
      } else {
        Cyril2Latin.getOrElse(c, c)
      }
    }.mkString
  }

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