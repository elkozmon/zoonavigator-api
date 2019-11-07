logLevel := Level.Warn

// Play
resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/maven-releases/"

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.7.3")

// Wartremover
addSbtPlugin("org.wartremover" % "sbt-wartremover" % "2.4.3")

// Scalafmt
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.2.1")
