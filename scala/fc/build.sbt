name := "fc"

import Dependencies._

libraryDependencies ++= Seq(
  cats,
  catsEffect,
  fs2,
  compilerDep(scalaVersion.value),
  refined,
  slf4j,
  spire,
  squants
)
