name := "fc"

import Dependencies._

libraryDependencies ++= Seq(
  cats,
  fs2,
  compilerDep(scalaVersion.value),
  refined,
  scalaTest % Test,
  scalaMock % Test
)
