
lazy val akkaHttpVersion = "10.0.9"
lazy val akkaVersion    = "2.5.2"

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "com.ra",
      scalaVersion := "2.12.3",
      version      := "0.1.0-SNAPSHOT"
    )),
    name := "Hello",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http"                % akkaHttpVersion,
      "org.json4s"        %% "json4s-native"            % "3.5.3",
      "org.json4s"        %% "json4s-jackson"           % "3.5.3",
      "com.typesafe.akka" %% "akka-stream"              % akkaVersion
    )
  )
