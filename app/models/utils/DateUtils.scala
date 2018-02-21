package models.utils

import java.time.{LocalDateTime, ZoneId}
import java.util.Date

object DateUtils {

	def toLocalDate(date: Date): LocalDateTime =
		LocalDateTime.ofInstant(date.toInstant, ZoneId.systemDefault)

	def toDate(ldt: LocalDateTime): Date =
		Date.from(ldt.atZone(ZoneId.systemDefault).toInstant)

	def addYearsToCurrentDate(years: Int) = {
		toDate(LocalDateTime.now().plusYears(years))
	}

}
