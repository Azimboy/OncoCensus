name := "OncoCensus"
 
version := "1.0" 
      
lazy val `oncocensus` = (project in file(".")).enablePlugins(PlayScala)

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"
      
resolvers += "Akka Snapshot Repository" at "http://repo.akka.io/snapshots/"
      
scalaVersion := "2.12.2"

val playVersion = "2.6.5"
val akkaVersion = "2.5.6"
val silhouetteVersion = "5.0.1"

val play = Seq(
	"com.typesafe.play" %% "play" % playVersion,
	"com.typesafe.play" %% "play-json" % playVersion,
	"com.typesafe.play" %% "play-test" % playVersion % "test",
	"com.typesafe.play" %% "play-slick" % "3.0.0",
	"com.typesafe.play" %% "play-slick" % "3.0.0",
	"com.typesafe.play" %% "play-slick-evolutions" % "3.0.0"
)

val akka = Seq(
	"com.typesafe.akka" %% "akka-actor" % akkaVersion,
	"com.typesafe.akka" %% "akka-remote" % akkaVersion,
	"com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
	//	"com.typesafe.akka" %% "akka-cluster" % akkaVersion
	//	"com.typesafe.akka" %% "akka-cluster-metrics" % akkaVersion
	//	"com.typesafe.akka" %% "akka-cluster-tools" % version
	"com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test"
)

val auth = Seq(
	"com.mohiva" %% "play-silhouette" % silhouetteVersion,
	"com.mohiva" %% "play-silhouette-password-bcrypt" % silhouetteVersion,
	"com.mohiva" %% "play-silhouette-persistence" % silhouetteVersion,
	"com.mohiva" %% "play-silhouette-crypto-jca" % silhouetteVersion,
	"com.mohiva" %% "play-silhouette-testkit"
)

val database = Seq(
	"org.postgresql" % "postgresql" % "42.0.0",
	"com.typesafe.slick" %% "slick" % "3.2.1",
	"com.github.tminglei" %% "slick-pg" % "0.15.1",
	"com.github.tminglei" %% "slick-pg_play-json" % "0.15.1",
	"com.h2database" % "h2" % "1.4.194"
)

val webJars = Seq(
	"org.webjars" %% "webjars-play" % "2.6.1", // playV
	"org.webjars" % "materializecss" % "0.100.2",
	"org.webjars" % "knockout" % "3.3.0",
	"org.webjars" % "lodash" % "3.10.1"
	//	"org.webjars" % "material-design-icons" % "2.2.0"
)

libraryDependencies ++= play ++ akka ++ database ++ webJars ++
	Seq(ehcache , ws , specs2 % Test , guice )

unmanagedResourceDirectories in Test <+=  baseDirectory ( _ /"target/web/public/test" )