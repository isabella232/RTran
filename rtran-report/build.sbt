import Versions._

libraryDependencies ++= Seq(
  "org.reflections" % "reflections" % "0.9.9-RC1",
  "ch.qos.logback" % "logback-classic" % logbackVersion,
  "org.scalatest" %% "scalatest" % scalatestVersion % "test",
  "com.typesafe.scala-logging" %% "scala-logging" % scalaLoggingVersion % "test"
)

coverageEnabled := true

makePomConfiguration ~= { config =>
  config.copy(process = TransformFilterBadDependencies)
}