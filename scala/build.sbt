organization in ThisBuild := "com.github.petekneller"

version in ThisBuild := "dev"

scalaVersion in ThisBuild := "2.12.8"


/* Ammonite */

libraryDependencies in ThisBuild += "com.lihaoyi" %% "ammonite" % "1.6.5" cross CrossVersion.full

initialCommands in (ThisBuild, console) := """ammonite.Main().run()"""


/* Ensime */

ensimeIgnoreMissingDirectories in ThisBuild := true


/* modules */

lazy val ioctl = project.in(file("ioctl"))

lazy val spidev = project.in(file("spidev")).
  dependsOn(ioctl)

lazy val fc = project.in(file("fc")).
  dependsOn(ioctl, spidev)

lazy val util = project.in(file("util")).
  dependsOn(ioctl, spidev, fc)

lazy val root = project.in(file(".")).
  aggregate(ioctl, spidev, fc, util)
