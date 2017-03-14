name := "fc"

import Dependencies._

libraryDependencies ++= Seq(
  cats,
  fs2,
  compilerDep(scalaVersion.value),
  refined,
  spire,
  scalaTest % Test,
  scalaMock % Test
)
