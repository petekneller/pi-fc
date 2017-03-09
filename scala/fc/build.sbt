name := "fc"

import Dependencies._

libraryDependencies ++= Seq(
  cats,
  fs2,
  scalaTest % Test,
  scalaMock % Test
)
