package models

import java.util.Date

import play.api.libs.json.Json

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
    name: String,
    districtId: Int
  )

	implicit val regionFormat = Json.format[Region]
	implicit val districtFormat = Json.format[District]
	implicit val departmentFormat = Json.format[Department]

	case object GetAllRegions
	case class GetDistrictsByRegionId(regionId: Int)
	case class GetDepartmentsByDistrictId(districtId: Int)

	case object GetDepartmentsReport
	case class CreateDepartment(department: Department)
	case class UpdateDepartment(department: Department)
	case class DeleteDepartment(departmentId: Int)

	case class DepartmentsReport(
    id: Option[Int],
    name: String,
		createdAt: Option[Date] = None,
		regionName: String,
		regionId: Int,
		districtName: String,
		districtId: Int
	)

	implicit val departmentsReportFormat = Json.format[DepartmentsReport]
}
