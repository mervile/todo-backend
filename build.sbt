val akka = "2.4.12"

val spray = "1.3.4"

lazy val commonSettings = Seq(
  organization := "mervile",
  version := "0.1.0",
  scalaVersion := "2.11.8"
)

lazy val root = (project in file(".")).
  settings(commonSettings: _*).
  settings(
    name := "hello",

    resolvers += "spray" at "http://repo.spray.io/",

    libraryDependencies ++=
        Seq(
            // -- Logging --
            "ch.qos.logback" % "logback-classic" % "1.1.2",
            "com.typesafe.scala-logging" %% "scala-logging-slf4j" % "2.1.2",
            // -- Akka --
            "com.typesafe.akka" %% "akka-testkit" % akka % "test",
            "com.typesafe.akka" %% "akka-actor" % akka,
            "com.typesafe.akka" %% "akka-slf4j" % akka,
            // -- Spray --
            "io.spray" %% "spray-routing" % spray,
            "io.spray" %% "spray-client" % spray,
            "io.spray" %% "spray-testkit" % spray % "test",
            // -- json --
            "io.spray" %% "spray-json" % "1.3.1",
            // -- config --
            "com.typesafe" % "config" % "1.2.1",
            // -- testing --
            "org.scalatest" %% "scalatest" % "2.2.1" % "test"
        )
)
