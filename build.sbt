import Dependencies._

ThisBuild / scalaVersion     := "2.12.8"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.example"
ThisBuild / organizationName := "example"

val fastparse    = "com.lihaoyi"        %% "fastparse"    % "2.1.0"
val handlebars   = "com.github.jknack"  % "handlebars"    % "4.1.2"
val commonsText  = "org.apache.commons" % "commons-text"  % "1.6"
val commonsCodec = "commons-codec"      % "commons-codec" % "1.12"
val json4s       = "org.json4s"         %% "json4s-core"  % "3.6.5"
val scalajack    = "co.blocke"          %% "scalajack"    % "6.0.1"

lazy val root = (project in file("."))
  .settings(
    name := "scalabars",
    libraryDependencies ++= Seq(fastparse, commonsText, scalajack, json4s, scalaTest % Test)
  )

// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.