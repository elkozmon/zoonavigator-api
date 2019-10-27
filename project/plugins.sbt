logLevel := Level.Warn

// Play
resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/maven-releases/"

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.7.1")

// Wartremover
addSbtPlugin("org.wartremover" % "sbt-wartremover" % "2.2.1")

// Scalafmt
addSbtPlugin("com.lucidchart" % "sbt-scalafmt" % "1.10")

// Release
addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.8")
