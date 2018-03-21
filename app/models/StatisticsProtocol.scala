package models

import models.AppProtocol.Paging.PageReq
import models.AppProtocol.ReportData
import models.PatientProtocol.ClientGroup

object StatisticsProtocol {

	case class PatientsReport(
    clientGroup: ClientGroup,
    maleCount: Int,
    femaleCount: Int
  )

	case class GetDetailedReport(reportData: ReportData, pageReq: PageReq)

}
