import java.net.URL

import de.heikoseeberger.sbtheader.license.AGPLv3

val commonSettings = Seq(
  organization := "com.elkozmon",
  version := "0.0.1-SNAPSHOT",
  headers := Map(
    "scala" -> AGPLv3("2017", "Ľuboš Kozmon")
  ),
  licenses += ("GNU Affero GPL V3", url("http://www.gnu.org/licenses/agpl-3.0.html")),
  developers := List(
    Developer(
      id = "elkozmon",
      name = "Ľuboš Kozmon",
      email = "lubosh91@gmail.com",
      url = new URL("http://www.elkozmon.com")
    )
  ),
  scalaVersion := "2.11.8",
  scalacOptions ++= Seq(
    "-target:jvm-1.8",
    "-encoding", "UTF-8",
    "-deprecation",
    "-feature",
    "-unchecked",
    "-Xlint",
    "-Ywarn-adapted-args",
    "-Ywarn-value-discard",
    "-Ywarn-inaccessible",
    "-Ywarn-dead-code"
  ),
  wartremoverErrors ++= Warts.unsafe
)

val core = (project in file("core"))
  .settings(commonSettings: _*)
  .settings(
    name := "zoonavigator-core",
    libraryDependencies ++= Seq(
      "org.slf4j" % "slf4j-api" % "1.7.24",
      "joda-time" % "joda-time" % "2.9.7",
      "org.apache.curator" % "curator-framework" % "2.11.1",
      "com.chuusai" %% "shapeless" % "2.3.2",
      "org.scalatest" %% "scalatest" % "3.0.1" % "test"
    )
  )
  .enablePlugins(AutomateHeaderPlugin)

val play = (project in file("play"))
  .settings(commonSettings: _*)
  .settings(
    name := "zoonavigator-play",
    libraryDependencies ++= Seq(
      filters,
      "org.typelevel" %% "cats" % "0.9.0",
      "ch.qos.logback" % "logback-classic" % "1.2.1",
      "com.google.guava" % "guava" % "16.0.1",
      "com.softwaremill.macwire" %% "macros" % "2.3.0" % "provided",
      "com.softwaremill.macwire" %% "util" % "2.3.0"
    )
  )
  .dependsOn(core)
  .enablePlugins(PlayScala, AutomateHeaderPlugin)
