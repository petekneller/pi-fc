import sbt._

object Dependencies {

  val scalaTest = "org.scalatest" %% "scalatest" % "3.0.1"

  val scalaMock = "org.scalamock" %% "scalamock-scalatest-support" % "3.5.0"

  val jna = "net.java.dev.jna" % "jna" % "4.3.0"

  val cats = "org.typelevel" %% "cats" % "0.9.0"

  val fs2Version = "0.9.4"
  val fs2 = "co.fs2" %% "fs2-core" % fs2Version

}
