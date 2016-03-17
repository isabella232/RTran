scalaVersion in ThisBuild := "2.11.8"

organization in ThisBuild := "org.rtran"

name := "rtran"

publishArtifact := false

parallelExecution in ThisBuild := false

fork in ThisBuild := true

lazy val `rtran-api` = project

lazy val `rtran-report-api` = project

lazy val `rtran-report` = project dependsOn `rtran-report-api`

lazy val `rtran-core` = project dependsOn `rtran-api`

lazy val `rtran-generic` = project dependsOn `rtran-api`

lazy val `rtran-maven` = project dependsOn (`rtran-generic`, `rtran-report-api`)

libraryDependencies in ThisBuild ++= Seq(
  "org.scala-lang.modules" %% "scala-java8-compat" % "0.5.0"
)

publishMavenStyle in ThisBuild := true

pomIncludeRepository in ThisBuild := { _ => false }

// disable using the Scala version in output paths and artifacts
crossPaths in ThisBuild := false

publishTo in ThisBuild := {
  val nexus = "https://oss.sonatype.org/"
  if (version.value.trim.endsWith("SNAPSHOT"))
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

publishMavenStyle in ThisBuild := true

publishArtifact in Test := false

pomIncludeRepository in ThisBuild := { _ => false }

pomExtra in ThisBuild :=
  <url>https://github.com/eBay/RTran</url>
    <licenses>
      <license>
        <name>Apache License, Version 2.0</name>
        <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
        <distribution>repo</distribution>
      </license>
    </licenses>
    <scm>
      <url>git@github.com:eBay/RTran.git</url>
      <connection>scm:git@github.com:eBay/RTran.git</connection>
    </scm>
    <developers>
      <developer>
        <id>zhuchenwang</id>
        <name>Zhuchen Wang</name>
        <url>https://github.com/zhuchenwang</url>
      </developer>
    </developers>