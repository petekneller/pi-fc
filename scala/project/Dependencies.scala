import sbt._

object Dependencies {

  val scalaTest = "org.scalatest" %% "scalatest" % "3.0.5"

  val scalaMock = "org.scalamock" %% "scalamock" % "4.1.0"

  val jna = "net.java.dev.jna" % "jna" % "4.3.0"

  val cats = "org.typelevel" %% "cats-core" % "1.6.0"
  val catsEffects = "org.typelevel" %% "cats-effect" % "1.2.0"

  val fs2 = "co.fs2" %% "fs2-core" % "0.9.4"

  def compilerDep(scalaVersion: String) = "org.scala-lang" % "scala-compiler" % scalaVersion % Provided

  val refined = "eu.timepit" %% "refined" % "0.9.4"

  val spire = "org.spire-math" %% "spire" % "0.13.0"

  val squants = "org.typelevel"  %% "squants"  % "1.4.0"

}
