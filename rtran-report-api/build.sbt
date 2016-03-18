coverageEnabled := false

makePomConfiguration ~= { config =>
  config.copy(process = TransformFilterBadDependencies)
}