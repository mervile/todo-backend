val akka = "2.4.16"
val akkaHttpV = "10.0.1"
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
        "com.typesafe.akka" %% "akka-http" % akkaHttpV,
        // -- json --
        "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpV,
        // -- config --
        "com.typesafe" % "config" % "1.3.1",
        // -- testing --
        "org.scalatest" %% "scalatest" % "3.0.1" % "test",
        "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpV % "test",
        // MongoDB Scala driver
        "org.mongodb" %% "casbah" % "3.1.1",
        // Password hashing
        "com.github.t3hnar" %% "scala-bcrypt" % "3.0",
        // JWT
        "com.pauldijou" %% "jwt-core" % "0.10.0",
        // CORS
        "ch.megard" %% "akka-http-cors" % "0.1.11"
      )
)
