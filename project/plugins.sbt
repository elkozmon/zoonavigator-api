logLevel := Level.Warn

// Play
resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/maven-releases/"

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.6.6")

// Wartremover
addSbtPlugin("org.wartremover" % "sbt-wartremover" % "2.2.1")

addSbtPlugin("org.danielnixon" % "sbt-playwarts" % "1.1.2")

// Scalafmt
addSbtPlugin("com.lucidchart" % "sbt-scalafmt" % "1.10")

// Release
addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.8")

// Build info
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.9.0")
