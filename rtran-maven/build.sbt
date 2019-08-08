import Versions._

val aetherVersion = "1.1.0"


libraryDependencies ++= Seq(
  "org.apache.maven" % "maven-core" % "3.6.1",
  "org.apache.maven.resolver" % "maven-resolver" % "1.3.3",
  "org.apache.maven.resolver" % "maven-resolver-connector-basic" % "1.3.3",
  "org.apache.maven.resolver" % "maven-resolver-transport-file" % "1.3.3",
  "org.apache.maven.resolver" % "maven-resolver-transport-http" % "1.3.3",
  "com.typesafe" % "config" % typesafeConfigVersion,
  "org.commonjava.maven" % "maven3-model-jdom-support" % "1.5",
  "commons-io" % "commons-io" % apacheCommonsIOVersion,
  "ch.qos.logback" % "logback-classic" % logbackVersion,
  "com.typesafe.scala-logging" %% "scala-logging" % scalaLoggingVersion,
  "org.scalatest" %% "scalatest" % scalatestVersion % "test"
)

cleanFiles += baseDirectory.value / "maven-repo"

makePomConfiguration ~= { config =>
  config.copy(process = TransformFilterBadDependencies)
}