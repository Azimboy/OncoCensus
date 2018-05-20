package models.utils

import java.io.{ByteArrayOutputStream, File}
import java.nio.file.{Files, Path}
import java.time.ZoneId
import java.util.Date

import com.typesafe.scalalogging.LazyLogging
import org.apache.poi.EncryptedDocumentException
import org.apache.poi.ss.usermodel._
import org.apache.poi.ss.util.{CellRangeAddress, CellRangeAddressList}
import org.apache.poi.xssf.usermodel._

import scala.collection.JavaConverters._

object FileUtils extends LazyLogging {

	def saveFile(sourcePath: Path, targetPath: Path, fileId: String): Unit = {
		Files.write(targetPath.resolve(fileId), Files.readAllBytes(sourcePath))
	}

	private val badCharsR = """\/|\.\.|\?|\*|:|\\""".r // / .. ? * : \

	case class SpreadsheetException(errorText: String) extends Exception(errorText)

	case class ExcelFileParam(
														 sheetName: String,
														 columnTitles: Seq[String],
														 rowValues: Seq[Seq[String]],
														 dropdownValues: Map[String, Seq[String]] = Map(),
														 startTitleRowFrom: Int = 0,
														 frozeColumnFrom: Int = 0
													 )

	def isCorrectFileName(name: String) = {
		badCharsR.findFirstIn(name).isEmpty
	}

	def parseSpreadsheet(file: File): Either[String, List[List[String]]] = {
		var matrix: List[List[String]] = Nil
		try {
			val wb = WorkbookFactory.create(file)
			for (i <- 0 until wb.getNumberOfSheets) {
				val sheet = wb.getSheetAt(i)
				for (row: Row <- sheet.rowIterator().asScala) {
					var rowValues = List[String]()
					for (cell: Cell <- row.cellIterator().asScala) {
						val cellValue = cell.toString.trim
						rowValues = rowValues :+ cellValue
					}
					if (rowValues.nonEmpty) {
						matrix = matrix :+ rowValues
					}
				}
			}
			Right(matrix)
		} catch {
			case error: Throwable =>
				logger.error(s"Error during parsing of ${file.getName}", error)
				Left("Faylni yuklashda xatolik.")
		}
	}

	private def getExcelRowHeight(strs: Seq[String], heightPerLine: Int) = {
		strs.map(_.split("\n").length).toList.max * heightPerLine
	}

	private def getExcelWorkbook(param: ExcelFileParam) = {
		val workbook = new XSSFWorkbook
		val sheet = workbook.createSheet(param.sheetName)
		if (param.frozeColumnFrom > 0) {
			sheet.createFreezePane(param.frozeColumnFrom, 0, param.frozeColumnFrom, 0)
		}

		val titleRow = sheet.createRow(param.startTitleRowFrom)
		val style = workbook.createCellStyle()
		val font = workbook.createFont()

		font.setBoldweight(Font.BOLDWEIGHT_BOLD)
		font.setFontHeightInPoints(11)
		style.setFont(font)
		style.setVerticalAlignment(CellStyle.VERTICAL_CENTER)
		style.setAlignment(CellStyle.ALIGN_CENTER)
		style.setWrapText(true) // microsoft office fix for \n (new line)
		titleRow.setHeightInPoints(getExcelRowHeight(param.columnTitles, 16))

		// Generate a header row (1 row)
		for ((columnTitle, index) <- param.columnTitles.view.zipWithIndex) {
			val cell = titleRow.createCell(index)
			cell.setCellValue(columnTitle)
			cell.setCellStyle(style)
			//      sheet.autoSizeColumn(index)
		}

		// Generate body rows (many rows)
		val cellStyle = workbook.createCellStyle()
		cellStyle.setVerticalAlignment(CellStyle.VERTICAL_CENTER)
		cellStyle.setWrapText(true)
		for ((rowValue, rowIndex) <- param.rowValues.view.zipWithIndex) {
			val rowNum = param.startTitleRowFrom + rowIndex + 1
			val row = sheet.createRow(rowNum)
			row.setHeightInPoints(getExcelRowHeight(rowValue, 15))

			for ((cellValue, cellIndex) <- rowValue.view.zipWithIndex) {
				if (cellValue.startsWith("DROPDOWN:")) {
					val cellKey = cellValue.replace("DROPDOWN:", "")
					val dvHelper = new XSSFDataValidationHelper(sheet)
					val dvConstraint = dvHelper.createExplicitListConstraint(param.dropdownValues(cellKey).toArray)
					val addressList = new CellRangeAddressList(rowNum, rowNum, cellIndex, cellIndex)
					val validation = dvHelper.createValidation(dvConstraint, addressList)
					validation.setSuppressDropDownArrow(true)
					sheet.addValidationData(validation)
				} else {
					val rowCell = row.createCell(cellIndex)
					rowCell.setCellValue(cellValue)
					rowCell.setCellStyle(cellStyle)
				}
			}
		}

		// Resize all columns, be careful, if there is very long text in column, it will result very long column
		for (index <- param.columnTitles.view.indices) {
			sheet.autoSizeColumn(index)
		}

		workbook
	}

	def workbookToByteArray(workbook: Workbook) = {
		val bos = new ByteArrayOutputStream
		try {
			workbook.write(bos)
		} catch {
			case error: Throwable =>
				logger.warn(s"Error during creating byte array for excel", error)
		} finally {
			bos.close()
		}
		bos.toByteArray
	}

	// Excel cell may contain dropdown. Example dropdown: rowValues = Seq(Seq("DROPDOWN:attestation"))
	// dropdownValues = Map("attestation" -> Seq("item1", "item2"))
	def getExcelAsBytes(param: ExcelFileParam) = {
		workbookToByteArray(getExcelWorkbook(param))
	}

	def getUserManagementExcelBytes(param: ExcelFileParam) = {
		val workbook = getExcelWorkbook(param.copy(startTitleRowFrom = 1, frozeColumnFrom = 4))
		val headerColor = new XSSFColor(Array(226, 240, 217).map(_.toByte))

		def getStyle(fontSize: Short) = {
			val style = workbook.createCellStyle()
			val font = workbook.createFont()
			font.setBoldweight(Font.BOLDWEIGHT_BOLD)
			font.setFontHeightInPoints(fontSize)
			style.setFont(font)
			style.setFillForegroundColor(headerColor)
			style.setFillPattern(CellStyle.SOLID_FOREGROUND)
			style
		}

		val sheet = workbook.getSheet(param.sheetName)
		sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3))
		sheet.addMergedRegion(new CellRangeAddress(0, 0, 4, 7))
		sheet.addMergedRegion(new CellRangeAddress(0, 0, 8, 11))

		val headerRow = sheet.createRow(0)
		headerRow.setHeightInPoints(30)

		val cellH1 = headerRow.createCell(0) // H1 for header 1
		cellH1.setCellValue("Cielo Mobile Platform")
		cellH1.setCellStyle(getStyle(24))

		val cellH2 = headerRow.createCell(4)
		cellH2.setCellValue("User Access Attestation")
		cellH2.setCellStyle(getStyle(18))

		val cellH3 = headerRow.createCell(8)
		val date = new Date
		val localDate = date.toInstant.atZone(ZoneId.systemDefault()).toLocalDate
		val year  = localDate.getYear
		val month = localDate.getMonthValue
		val quarter = Math.ceil(month / 3.0).toInt
		cellH3.setCellValue(s"Q$quarter FY$year")
		cellH3.setCellStyle(getStyle(14))

		// Set background colors for column titles
		val titleRow = sheet.getRow(1)
		for (index <- param.columnTitles.view.indices) {
			val titleCell = titleRow.getCell(index)
			val style = titleCell.getCellStyle
			style.setFillForegroundColor(headerColor)
			style.setFillPattern(CellStyle.SOLID_FOREGROUND)
			titleCell.setCellStyle(style)
		}

		workbookToByteArray(workbook)
	}

	def getBytesFromPath(filePath: Path): Array[Byte] = {
		Files.readAllBytes(filePath)
	}
}
