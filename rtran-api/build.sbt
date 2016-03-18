libraryDependencies ++= Seq(
  "junit" % "junit" % "4.12" % "test",
  "com.novocode" % "junit-interface" % "0.11" % "test"
)

coverageEnabled := false

makePomConfiguration ~= { config =>
  config.copy(process = TransformFilterBadDependencies)
}