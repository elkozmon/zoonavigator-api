scalaVersion in ThisBuild := "2.13.0"

scalacOptions in ThisBuild ++= Seq(
  "-encoding", "UTF-8",
  "-deprecation",
  "-feature",
  "-unchecked",
  "-Xlint:adapted-args,inaccessible",
  "-Wvalue-discard",
  "-Wdead-code"
)

val catsVersion = "2.0.0"
val curatorVersion = "4.0.0"
val macwireVersion = "2.3.3"

val commonSettings = Seq(
  organization := "com.elkozmon",
  licenses += ("GNU Affero GPL V3", url(
    "http://www.gnu.org/licenses/agpl-3.0.html"
  )),
  developers := List(
    Developer(
      id = "elkozmon",
      name = "Ľuboš Kozmon",
      email = "lubosh91@gmail.com",
      url = url("http://www.elkozmon.com")
    )
  ),
  libraryDependencies ++= Seq(
    "org.typelevel" %% "cats-core" % catsVersion,
    "org.typelevel" %% "cats-free" % catsVersion,
    "org.scalatest" %% "scalatest" % "3.0.8" % Test
  ),
  wartremoverErrors := Warts.unsafe.filterNot(_.eq(Wart.Any))
)

val core = project
  .settings(commonSettings: _*)
  .settings(
    name := "zoonavigator-core",
    libraryDependencies ++= Seq(
      "org.slf4j" % "slf4j-api" % "1.7.25",
      "joda-time" % "joda-time" % "2.9.9",
      "org.apache.curator" % "curator-framework" % curatorVersion exclude("org.apache.zookeeper", "zookeeper"),
      "org.apache.curator" % "curator-test" % curatorVersion % Test,
      "org.apache.zookeeper" % "zookeeper" % "3.4.11" exclude("org.slf4j", "slf4j-log4j12"),
      "io.monix" %% "monix-eval" % "3.0.0",
      "com.chuusai" %% "shapeless" % "2.3.3"
    )
  )

val play = project
  .enablePlugins(PlayScala)
  .settings(commonSettings: _*)
  .settings(
    name := "zoonavigator-play",
    libraryDependencies ++= Seq(
      filters,
      "ch.qos.logback" % "logback-classic" % "1.2.3",
      "com.softwaremill.macwire" %% "macros" % macwireVersion % Provided,
      "com.softwaremill.macwire" %% "util" % macwireVersion
    ),
    routesImport ++= Seq(
      "binders._",
      "com.elkozmon.zoonavigator.core.zookeeper.znode.ZNodePath",
      "com.elkozmon.zoonavigator.core.zookeeper.znode.ZNodeAclVersion",
      "com.elkozmon.zoonavigator.core.zookeeper.znode.ZNodeDataVersion"
    ),
    wartremoverExcluded ++= routes.in(Compile).value,
    sources in (Compile, doc) := Seq.empty,
    publishArtifact in (Compile, packageDoc) := false
  )
  .dependsOn(core)
