package models

import java.text.SimpleDateFormat
import java.util.Date

import models.AppProtocol.Paging.PageReq
import models.PatientProtocol.{ClientGroup, PatientsFilter}
import play.api.libs.functional.syntax._
import play.api.libs.json.{Format, Json, OFormat, __}

import scala.util.Try

object AppProtocol {

	case class Region(
    id: Option[Int] = None,
    name: String
  )

	case class District(
	  id: Option[Int] = None,
	  name: String,
	  regionId: Int
  )

	case class Department(
    id: Option[Int] = None,
    createdAt: Option[Date] = None,
    name: String,
    districtId: Int,
    region: Option[Region] = None,
    district: Option[District] = None
  )

	implicit val regionFormat = Json.format[Region]
	implicit val districtFormat = Json.format[District]
	implicit val departmentFormat = Json.format[Department]

	case object GetAllRegions
	case object GetAllDistricts
	case class GetDepartmentsByDistrictId(districtId: Int)

	case object GetDepartmentsReport
	case class CreateDepartment(department: Department)
	case class UpdateDepartment(department: Department)
	case class DeleteDepartment(departmentId: Int)

	object Paging {
		case class PageReq(page: Int = 1,
			                 size: Int = 30,
			                 sortFields: Option[List[String]] = None,
			                 isPagination: Boolean = true,
			                 sortDirections: Option[List[String]] = None) {
			def offset = (page - 1) * size

			def toPageRes[T](items: Seq[T]) = {
				val pageItems =
					if (isPagination) {
						items.slice(offset, offset + size)
					} else {
						items
					}
				PageRes(items = pageItems, total = items.size)
			}
		}

		case class PageRes[T](items: Seq[T], total: Long)

		implicit def pageFormat[T: Format]: Format[PageRes[T]] = (
			(__ \ "items").format[Seq[T]] ~
			(__ \ "total").format[Long]
		)(PageRes.apply, unapply(PageRes.unapply))
	}

	case class ReportData(
		startDate: Option[Date] = None,
		endDate: Option[Date] = None,
		receiveType: Option[String] = None,
		regionId: Option[Int] = None,
		districtId: Option[Int] = None
	)

	case class PatientsReport(
    clientGroup: ClientGroup,
    maleCount: Int,
    femaleCount: Int
  )

	case class GetDetailedReport(reportData: ReportData, pageReq: PageReq)

	implicit val reportDataFormat: Format[ReportData] = (
		optDateFormat("startDate") ~
		optDateFormat("endDate") ~
		(__ \ "receiveType").formatNullable[String] ~
		(__ \ "regionId").formatNullable[Int] ~
		(__ \ "districtId").formatNullable[Int]
	)(ReportData.apply, unlift(ReportData.unapply))

	def dateFormat(fieldName: String, dateFormat: String = "dd.MM.yyyy HH:mm") = {
		OFormat(
			(__ \ fieldName).read[String].map(new SimpleDateFormat(dateFormat).parse),
			(__ \ fieldName).write[Date]
		)
	}

	def optDateFormat(fieldName: String, dateFormat: String = "dd.MM.yyyy HH:mm") = {
		OFormat(
			(__ \ fieldName).readNullable[String].map(_.flatMap(s => Try(new SimpleDateFormat(dateFormat).parse(s)).toOption)),
			(__ \ fieldName).writeNullable[Date]
		)
	}
}
