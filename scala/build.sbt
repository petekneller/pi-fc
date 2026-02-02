ThisBuild / organization := "com.github.petekneller"

ThisBuild / version := "dev"

ThisBuild / scalaVersion := "2.13.18"

ThisBuild / scalacOptions ++= Seq(
  // Standard warnings
  "-deprecation",           // Warn about deprecated APIs
  "-feature",               // Warn about features requiring explicit enabling
  "-unchecked",             // Warn about unchecked type operations

  // Extra warnings useful for migration
  "-Xlint:adapted-args",          // Warn if argument list is adapted
  "-Xlint:infer-any",             // Warn when Any is inferred
  "-Xlint:missing-interpolator",  // Possible string interpolator missed
  "-Xlint:nullary-unit",          // Warn about nullary methods returning Unit
  "-Xlint:private-shadow",        // Private field shadowing local
  "-Xlint:type-parameter-shadow", // Type parameter shadows existing type

  // Unused code detection
  "-Ywarn-dead-code",
  "-Ywarn-unused:imports",
  "-Ywarn-unused:locals",
  "-Ywarn-unused:privates",

  // Value handling
  "-Ywarn-value-discard",   // Warn when non-Unit values are discarded
  "-Ywarn-numeric-widen"    // Warn about implicit numeric widening
)

/* Ammonite */

ThisBuild / libraryDependencies += "com.lihaoyi" %% "ammonite" % "3.0.8" cross CrossVersion.full

ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always

def ammonite() = {
  Test / sourceGenerators += Def.task {
    val file = (Test / sourceManaged).value / "amm.scala"
    IO.write(file, """object amm extends App { ammonite.AmmoniteMain.main(args) }""")
    Seq(file)
  }.taskValue
}

/* modules */

lazy val ioctl = project.in(file("ioctl"))

lazy val spidev = project.in(file("spidev")).
  dependsOn(ioctl)

lazy val core = project.in(file("core")).
  settings(ammonite()).
  dependsOn(ioctl, spidev)

lazy val util = project.in(file("util")).
  settings(ammonite()).
  dependsOn(ioctl, spidev, core)

lazy val root = project.in(file(".")).
  aggregate(ioctl, spidev, core, util)
