import Dependencies._

ThisBuild / scalaVersion := "2.13.4"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "org.smartjava"
ThisBuild / organizationName := "smartjava"

// need to use an older version, since the newest version of http4s
// supports the latest cats version, while the zio-interop-cats one
// doesn't support this yet.
val Http4sVersion = "1.0.0-M4"

lazy val root = (project in file("."))
  .settings(
    name := "zio-playground",
    libraryDependencies += scalaTest % Test,
    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-blaze-server" % Http4sVersion,
      "org.http4s" %% "http4s-dsl" % Http4sVersion,
      "dev.zio" %% "zio" % "1.0.6",
      "dev.zio" %% "zio-streams" % "1.0.6",
      "dev.zio" %% "zio-interop-cats" % "2.4.0.0",
      "com.github.pureconfig" %% "pureconfig" % "0.15.0",
      "org.slf4j" % "slf4j-api" % "1.7.5",
      "org.slf4j" % "slf4j-simple" % "1.7.5",
      "dev.zio" %% "zio-test" % "1.0.6" % "test",
      "dev.zio" %% "zio-test-sbt" % "1.0.6" % "test"
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )
