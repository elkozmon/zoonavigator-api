logLevel := Level.Warn

// Dependency graph
addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.8.2")

// Play
resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/maven-releases/"

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.6.2")

// Wartremover
addSbtPlugin("org.wartremover" % "sbt-wartremover" % "2.0.2")

addSbtPlugin("org.danielnixon" % "sbt-ignore-play-generated" % "0.1")

// SbtHeader
addSbtPlugin("de.heikoseeberger" % "sbt-header" % "1.8.0")

// SbtLicenseReport
addSbtPlugin("com.typesafe.sbt" % "sbt-license-report" % "1.2.0")
