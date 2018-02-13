package models.utils

import java.nio.file.{Files, Path}

object FileUtils {

	def saveFile(sourcePath: Path, targetPath: Path, fileId: String): Unit = {
		Files.write(targetPath.resolve(fileId), Files.readAllBytes(sourcePath))
	}

}
