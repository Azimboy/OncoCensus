package models.utils

import java.util.Date

import slick.jdbc.PostgresProfile.api._

trait EnumMappedToDb extends Enumeration {
  implicit def enumMapper = MappedColumnType.base[Value, Int](_.id, this.apply)

//  implicit val messageTypeMapper = MappedColumnType.base[Value, String](
//    e => e.toString,
//    s => Value(s)
//  )
}

trait Date2SqlDate {
  implicit val date2SqlDate = MappedColumnType.base[Date, java.sql.Timestamp](
    d => new java.sql.Timestamp(d.getTime),
    d => new java.util.Date(d.getTime)
  )
}
