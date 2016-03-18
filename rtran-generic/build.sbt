import Versions._

libraryDependencies ++= Seq(
  "org.json4s" %% "json4s-jackson" % json4sVersion,
  "org.json4s" %% "json4s-ext" % json4sVersion,
  "com.typesafe.scala-logging" %% "scala-logging" % scalaLoggingVersion,
  "commons-io" % "commons-io" % apacheCommonsIOVersion,
  "org.apache.commons" % "commons-lang3" % "3.4",
  "org.apache.ws.commons.axiom" % "axiom-api" % "1.2.15",
  "org.apache.ws.commons.axiom" % "axiom-impl" % "1.2.15",
  "com.googlecode.juniversalchardet" % "juniversalchardet" % "1.0.3",
  "org.scalatest" %% "scalatest" % scalatestVersion % "test"
)

makePomConfiguration ~= { config =>
  config.copy(process = TransformFilterBadDependencies)
}