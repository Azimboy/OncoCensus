package models.utils

import play.api.libs.json._

object EnumUtils {
	def enumReads[E <: EnumMappedToDb](enum: E): Reads[E#Value] = new Reads[E#Value] {
		def reads(json: JsValue): JsResult[E#Value] = json match {
			case JsString(s) => {
				try {
					JsSuccess(enum.withName(s))
				} catch {
					case _: NoSuchElementException => JsError(s"Enumeration expected of type: '${enum.getClass}', but it does not appear to contain the value: '$s'")
				}
			}
			case JsNumber(id) => {
				try {
					JsSuccess(enum.apply(id.intValue))
				} catch {
					case _: NoSuchElementException => JsError(s"Enumeration expected of type: '${enum.getClass}', but it does not appear to contain the value: '$id.intValue'")
				}
			}
			case _ => JsError("String value expected")
		}
	}

	implicit def enumWrites[E <: EnumMappedToDb](enum: E): Writes[E#Value] = new Writes[E#Value] {
		def writes(v: E#Value): JsValue = JsString(v.toString)
		//    def writes(v: E#Value): JsValue = JsString(enum.getI18eName(v.toString))
	}

	implicit def enumFormat[E <: EnumMappedToDb](enum: E): Format[E#Value] = {
		Format(EnumUtils.enumReads(enum), EnumUtils.enumWrites(enum))
	}
}