import ReleaseTransformations._

scalafmtVersion in ThisBuild := "1.1.0"

scalaVersion in ThisBuild := "2.12.4"

scalacOptions in ThisBuild ++= Seq(
  "-target:jvm-1.8",
  "-encoding",
  "UTF-8",
  "-deprecation",
  "-feature",
  "-unchecked",
  "-Xlint",
  "-Ywarn-adapted-args",
  "-Ywarn-value-discard",
  "-Ywarn-inaccessible",
  "-Ywarn-dead-code"
)

releaseTagName := (version in ThisBuild).value

releaseTagComment := s"Version ${(version in ThisBuild).value}"

releaseCommitMessage := s"Set version to ${(version in ThisBuild).value}"

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  setNextVersion,
  commitNextVersion,
  pushChanges
)

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
    "org.typelevel" %% "cats" % "0.9.0",
    "org.scalatest" %% "scalatest" % "3.0.4" % Test
  ),
  wartremoverErrors ++= Warts.unsafe
)

val core = project
  .settings(commonSettings: _*)
  .settings(
    name := "zoonavigator-core",
    libraryDependencies ++= Seq(
      "org.slf4j" % "slf4j-api" % "1.7.25",
      "joda-time" % "joda-time" % "2.9.9",
      "org.apache.curator" % "curator-framework" % "4.0.0" exclude("org.apache.zookeeper", "zookeeper"),
      "org.apache.curator" % "curator-test" % "4.0.0" % Test,
      "org.apache.zookeeper" % "zookeeper" % "3.4.11" exclude("org.slf4j", "slf4j-log4j12"),
      "io.monix" %% "monix-eval" % "2.3.2",
      "io.monix" %% "monix-cats" % "2.3.2",
      "com.chuusai" %% "shapeless" % "2.3.2"
    )
  )

val play = project
  .enablePlugins(PlayScala, BuildInfoPlugin)
  .settings(commonSettings: _*)
  .settings(
    name := "zoonavigator-play",
    libraryDependencies ++= Seq(
      filters,
      "ch.qos.logback" % "logback-classic" % "1.2.3",
      "com.softwaremill.macwire" %% "macros" % "2.3.0" % Provided,
      "com.softwaremill.macwire" %% "util" % "2.3.0"
    ),
    buildInfoKeys := Seq[BuildInfoKey](
      name,
      version,
      scalaVersion,
      sbtVersion
    ),
    buildInfoPackage := "build",
    routesImport ++= Seq(
      "binders._",
      "com.elkozmon.zoonavigator.core.zookeeper.znode.ZNodePath",
      "com.elkozmon.zoonavigator.core.zookeeper.znode.ZNodeAclVersion",
      "com.elkozmon.zoonavigator.core.zookeeper.znode.ZNodeDataVersion"
    ),
    wartremoverExcluded ++= routes.in(Compile).value,
    wartremoverWarnings ++= Seq(
      PlayWart.AssetsObject,
      PlayWart.CookiesPartial,
      PlayWart.FlashPartial,
      PlayWart.FormPartial,
      PlayWart.HeadersPartial,
      PlayWart.JavaApi,
      PlayWart.JsLookupResultPartial,
      PlayWart.JsReadablePartial,
      PlayWart.LangObject,
      PlayWart.MessagesObject,
      PlayWart.SessionPartial,
      PlayWart.TypedMapPartial
    ),
    sources in (Compile, doc) := Seq.empty,
    publishArtifact in (Compile, packageDoc) := false
  )
  .dependsOn(core)
