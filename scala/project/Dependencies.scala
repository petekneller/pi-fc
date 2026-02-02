import sbt._

object Dependencies {

  val scalaTest = "org.scalatest" %% "scalatest" % "3.2.19"

  val scalaMock = "org.scalamock" %% "scalamock" % "5.2.0"

  val jna = "net.java.dev.jna" % "jna" % "5.3.1"

  val cats = "org.typelevel" %% "cats-core" % "2.12.0"
  val catsEffect = "org.typelevel" %% "cats-effect" % "3.5.7"

  val fs2 = "co.fs2" %% "fs2-core" % "3.11.0"
  val fs2io = "co.fs2" %% "fs2-io" % "3.11.0"

  def compilerDep(scalaVersion: String) = "org.scala-lang" % "scala-compiler" % scalaVersion % Provided

  val refined = "eu.timepit" %% "refined" % "0.11.2"

  val spire = "org.typelevel" %% "spire" % "0.17.0"

  val squants = "org.typelevel"  %% "squants"  % "1.8.3"

  val slf4j = "org.slf4j" % "slf4j-api" % "1.7.36"
  val logback = "ch.qos.logback" % "logback-classic" % "1.2.13"

}
