organization in ThisBuild := "com.github.petekneller"

version in ThisBuild := "dev"

scalaVersion in ThisBuild := "2.12.8"


/* Ammonite */

libraryDependencies in ThisBuild += "com.lihaoyi" %% "ammonite" % "1.6.5" cross CrossVersion.full

def ammonite() = {
  sourceGenerators in Test += Def.task {
    val file = (sourceManaged in Test).value / "amm.scala"
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
