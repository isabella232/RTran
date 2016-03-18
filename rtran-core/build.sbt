import Versions._

libraryDependencies ++= Seq(
  "org.json4s" %% "json4s-jackson" % "3.2.11",
  "com.typesafe.scala-logging" %% "scala-logging" % scalaLoggingVersion,
  "com.typesafe" % "config" % typesafeConfigVersion,
  "commons-io" % "commons-io" % apacheCommonsIOVersion % "test",
  "org.scalatest" %% "scalatest" % scalatestVersion % "test",
  "ch.qos.logback" % "logback-classic" % logbackVersion % "test"
)

coverageEnabled := true

makePomConfiguration ~= { config =>
  config.copy(process = TransformFilterBadDependencies)
}