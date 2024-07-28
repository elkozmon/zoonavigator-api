logLevel := Level.Warn

// Play
resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/maven-releases/"

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.8.22")

// Scalafmt
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.2.1")

// Scalafix
addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.10.1")