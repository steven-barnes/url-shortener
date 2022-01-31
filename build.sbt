name := """url-shortener"""
organization := "seb"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.8"

libraryDependencies ++= Seq(
  guice,
  "net.debasishg" %% "redisclient" % "3.41",
  "com.aventrix.jnanoid" % "jnanoid" % "2.0.0",

  "org.scalatest" %% "scalatest-funsuite" % "3.2.10" % Test,
  "org.scalatestplus" %% "mockito-3-4" % "3.2.10.0" % Test,
  "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % Test,
  "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % Test
)


// Adds additional packages into Twirl
//TwirlKeys.templateImports += "seb.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "seb.binders._"
