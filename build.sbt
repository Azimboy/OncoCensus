import sbt.ModuleID

name := "OncoCensus"

version := "1.0" 
      
lazy val `oncocensus` = (project in file(".")).enablePlugins(PlayScala)

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

scalaVersion := "2.12.4"

includeFilter in (Assets, LessKeys.less) := "*.less"
excludeFilter in (Assets, LessKeys.less) := "_*.less"

val playVersion = "2.6.8"
val akkaVersion = "2.5.13"
val slickVersion = "3.2.3"

val play: Seq[ModuleID] = Seq(
	"com.typesafe.play" %% "play-json" % playVersion,
)

val akka: Seq[ModuleID] = Seq(
	"com.typesafe.akka" %% "akka-actor" % akkaVersion,
	"com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
	"com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test"
)

val tools: Seq[ModuleID] = Seq(
	"org.apache.poi" % "poi" % "3.13",
	"org.apache.poi" % "poi-ooxml" % "3.13",
	"org.apache.commons" % "commons-text" % "1.1"
)

val database: Seq[ModuleID] = Seq(
	// SQL generator
	"com.typesafe.slick" %% "slick" % slickVersion,

	// Postgres driver
	"org.postgresql" % "postgresql" % "42.1.4",

	// Migration for SQL databases
	"org.flywaydb" % "flyway-core" % "5.0.7",

	// Play modules for Flyway
	"org.flywaydb" %% "flyway-play" % "5.0.0",

// Connection pool for database
	"com.zaxxer" % "HikariCP" % "2.7.0",
//	JSONB supporting on postgres
	"com.github.tminglei" %% "slick-pg" % "0.15.1",
	"com.github.tminglei" %% "slick-pg_play-json" % "0.15.1"
)

val loggingLibs: Seq[ModuleID] = Seq(
	"com.typesafe.scala-logging" % "scala-logging_2.12" % "3.7.2",
	"ch.qos.logback" % "logback-classic" % "1.1.7",
	"ch.qos.logback" % "logback-core" % "1.1.7",
	"org.slf4j" % "log4j-over-slf4j" % "1.7.21",
	"org.codehaus.janino" % "janino" % "3.0.7"
)

val webJars: Seq[ModuleID] = Seq(
	"org.webjars" %% "webjars-play" % "2.6.1", // playV
	"org.webjars" % "knockout" % "3.3.0",
	"org.webjars" % "lodash" % "3.10.1",
	"org.webjars" % "toastr" % "2.1.2",
	"org.webjars" % "bootstrap-select" % "1.7.3-1",
	"org.webjars" % "Eonasdan-bootstrap-datetimepicker" % "4.15.35",
	"org.webjars" % "momentjs" % "2.8.1",
	"org.webjars" % "jquery-ui" % "1.11.4",
	"org.webjars" % "jquery-ui-src" % "1.11.4",
	"org.webjars" % "jquery-file-upload" % "9.10.1",
	"org.webjars" % "es5-shim" % "4.1.14",
	"org.webjars" % "webshim" % "1.15.8",
	"org.webjars" % "requirejs" % "2.2.0",
	"org.webjars" % "modernizr" % "2.8.3",
	"org.webjars.bower" % "twbs-pagination" % "1.2.5" exclude("org.webjars.bower", "jquery")
	//	"org.webjars" % "material-design-icons" % "2.2.0"
)

libraryDependencies ++= play ++ akka ++ tools ++ database ++ webJars ++ loggingLibs ++
	Seq(ws, specs2 % Test, guice, filters)