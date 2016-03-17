import Versions._

val aetherVersion = "1.0.2.v20150114"

libraryDependencies ++= Seq(
  "org.eclipse.aether" % "aether-api" % aetherVersion,
  "org.eclipse.aether" % "aether-spi" % aetherVersion,
  "org.eclipse.aether" % "aether-util" % aetherVersion,
  "org.eclipse.aether" % "aether-impl" % aetherVersion,
  "org.eclipse.aether" % "aether-connector-basic" % aetherVersion,
  "org.eclipse.aether" % "aether-transport-wagon" % aetherVersion,
  "org.eclipse.aether" % "aether-transport-classpath" % aetherVersion,
  "org.eclipse.aether" % "aether-transport-file" % aetherVersion,
  "org.eclipse.aether" % "aether-transport-http" % aetherVersion
)

libraryDependencies ++= Seq(
  "org.apache.maven" % "maven-core" % "3.3.3",
  "com.typesafe" % "config" % typesafeConfigVersion,
  "org.commonjava.maven" % "maven-model-jdom-support" % "3.0.3",
  "commons-io" % "commons-io" % apacheCommonsIOVersion,
  "ch.qos.logback" % "logback-classic" % logbackVersion,
  "com.typesafe.scala-logging" %% "scala-logging" % scalaLoggingVersion,
  "org.scalatest" %% "scalatest" % scalatestVersion % "test"
)

cleanFiles += baseDirectory.value / "maven-repo"

coverageEnabled := true