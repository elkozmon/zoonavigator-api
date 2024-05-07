import sbt._

object Dependencies {
  private[Dependencies] object V {
    val cats      = "2.2.0"
    val curator   = "4.2.0"
    val curatorTest = "2.12.0"
    val macwire   = "2.3.3"
    val zookeeper = "3.4.11"
    val log4j     = "2.17.2"
    val slf4j     = "1.7.25"
    val shapeless = "2.3.3"
    val monix     = "3.0.0"
    val commonsIo = "2.6"
    val logback   = "1.2.3"
    val jsoup     = "1.13.1"
    val scalaTest = "3.0.8"
    val scalafixOrganizeImports = "0.6.0"
  }

  val catsCore = "org.typelevel" %% "cats-core" % V.cats
  val catsFree = "org.typelevel" %% "cats-free" % V.cats

  val macwireUtil   = "com.softwaremill.macwire" %% "util"   % V.macwire
  val macwireProxy  = "com.softwaremill.macwire" %% "proxy"  % V.macwire
  val macwireMacros = "com.softwaremill.macwire" %% "macros" % V.macwire % Provided

  val curatorTest =
    "org.apache.curator" % "curator-test" % V.curatorTest % Test exclude ("org.apache.zookeeper", "zookeeper")
  val curatorFramework =
    "org.apache.curator" % "curator-framework" % V.curator exclude ("org.apache.zookeeper", "zookeeper")

  val zookeeper = "org.apache.zookeeper" % "zookeeper" % V.zookeeper exclude ("log4j", "log4j")

  val monixEval = "io.monix" %% "monix-eval" % V.monix

  val shapeless = "com.chuusai" %% "shapeless" % V.shapeless

  val commonsIo = "commons-io" % "commons-io" % V.commonsIo

  val logbackClassic = "ch.qos.logback" % "logback-classic" % V.logback

  val slf4jApi = "org.slf4j" % "slf4j-api" % V.slf4j

  val log4jApi  = "org.apache.logging.log4j" % "log4j-1.2-api" % V.log4j
  val log4jCore = "org.apache.logging.log4j" % "log4j-core"    % V.log4j

  val jsoup = "org.jsoup" % "jsoup" % V.jsoup

  val scalaTest = "org.scalatest" %% "scalatest" % V.scalaTest % Test

  val scalafixOrganizeImports = "com.github.liancheng" %% "organize-imports" % V.scalafixOrganizeImports
}
