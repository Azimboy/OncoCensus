package models

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

	implicit val regionFormat = Json.format[Region]
	implicit val districtFormat = Json.format[District]
}
