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
	case class GetDistrictsByRegionId(regionId: Int)
	case class GetDepartmentsByDistrictId(districtId: Int)

	case object GetDepartmentsReport
	case class CreateDepartment(department: Department)
	case class UpdateDepartment(department: Department)
	case class DeleteDepartment(departmentId: Int)

}
