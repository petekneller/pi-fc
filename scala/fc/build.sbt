name := "fc"

import Dependencies._

libraryDependencies ++= Seq(
  cats,
  scalaTest % Test,
  scalaMock % Test
)
