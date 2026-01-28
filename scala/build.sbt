ThisBuild / organization := "com.github.petekneller"

ThisBuild / version := "dev"

ThisBuild / scalaVersion := "2.12.18"

/* Ammonite */

ThisBuild / libraryDependencies += "com.lihaoyi" %% "ammonite" % "3.0.8" cross CrossVersion.full

ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always

def ammonite() = {
  Test / sourceGenerators += Def.task {
    val file = (Test / sourceManaged).value / "amm.scala"
    IO.write(file, """object amm extends App { ammonite.Main.main(args) }""")
    Seq(file)
  }.taskValue
}

/* modules */

lazy val ioctl = project.in(file("ioctl"))

lazy val spidev = project.in(file("spidev")).
  dependsOn(ioctl)

lazy val fc = project.in(file("fc")).
  settings(ammonite()).
  dependsOn(ioctl, spidev)

lazy val util = project.in(file("util")).
  settings(ammonite()).
  dependsOn(ioctl, spidev, fc)

lazy val root = project.in(file(".")).
  aggregate(ioctl, spidev, fc, util)
