// The Play plugin
resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.6.11")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.5.1")
//addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.5")
addSbtPlugin("com.lightbend.sbt" % "sbt-aspectj" % "0.11.0")
addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.2.2")
addSbtPlugin("com.lightbend.sbt" % "sbt-javaagent" % "0.1.4")

// web plugins
addSbtPlugin("com.typesafe.sbt" % "sbt-coffeescript" % "1.0.2")
addSbtPlugin("com.typesafe.sbt" % "sbt-less" % "1.1.2")
//addSbtPlugin("com.typesafe.sbt" % "sbt-jshint" % "1.0.5")
addSbtPlugin("com.typesafe.sbt" % "sbt-rjs" % "1.0.10")
addSbtPlugin("com.typesafe.sbt" % "sbt-digest" % "1.1.3")
addSbtPlugin("com.typesafe.sbt" % "sbt-mocha" % "1.1.2")