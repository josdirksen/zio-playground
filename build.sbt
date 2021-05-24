import Dependencies._

ThisBuild / scalaVersion := "2.13.4"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "org.smartjava"
ThisBuild / organizationName := "smartjava"

// need to use an older version, since the newest version of http4s
// supports the latest cats version, while the zio-interop-cats one
// doesn't support this yet.
val Http4sVersion = "1.0.0-M4"
val ZioVersion = "1.0.8"

lazy val root = (project in file("."))
  .settings(
    name := "zio-playground",
    libraryDependencies += scalaTest % Test,
    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-blaze-server" % Http4sVersion,
      "org.http4s" %% "http4s-dsl" % Http4sVersion,
      "org.http4s" %% "http4s-blaze-client" % Http4sVersion,
      "org.http4s" %% "http4s-circe" % Http4sVersion,
      // JSON Mapping
      "io.circe" %% "circe-generic" % "0.12.3",
      "io.circe" %% "circe-literal" % "0.12.3",
      // ZIO stuff
      "dev.zio" %% "zio" % ZioVersion,
      "dev.zio" %% "zio-streams" % ZioVersion,
      "dev.zio" %% "zio-streams" % ZioVersion,
      "dev.zio" %% "zio-interop-reactivestreams" % "1.3.5",
      "dev.zio" %% "zio-interop-cats" % "2.4.0.0",
      "com.github.pureconfig" %% "pureconfig" % "0.15.0",
      "org.slf4j" % "slf4j-api" % "1.7.5",
      "org.slf4j" % "slf4j-simple" % "1.7.5",
      "dev.zio" %% "zio-test" % ZioVersion % "test",
      "dev.zio" %% "zio-test-sbt" % ZioVersion % "test",
      "org.mongodb.scala" %% "mongo-scala-driver" % "4.2.3"
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )
