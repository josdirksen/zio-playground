package example

import zio.ExitCode
import zio.URIO
import zio.ZEnv
import configuration.Configuration
import example.services.http4sServer
import zio.ZIO
import example.services.tempClient
import zio.stream.ZSink
import example.services.http4sClient

/** Base entry point for a ZIO app, which runs the logic within
  * a specified environment
  */
object Application extends zio.App {

  // combine the configuration layer and the standard environment into
  // a new layer.
  val defaultLayer = Configuration.live ++ ZEnv.live
  // the complete application layer also consists out of the http4server, so
  // add that to the default layers list
  // val applicationLayer =
  //   (defaultLayer >>> http4sServer.Http4sServer.live) ++
  //     (defaultLayer >>> tempClient.TempClient.live) ++ ZEnv.live

  val applicationLayer =
    (defaultLayer >>> http4sServer.Http4sServer.live) ++
      (http4sClient.Http4sClient.live >>> tempClient.TempClient.live) ++
      ZEnv.live ++
      http4sClient.Http4sClient.live

  // now run the application, by providing it with the application layer
  // override def run(args: List[String]): URIO[ZEnv, ExitCode] =
  //   myAppLogic.provideLayer(applicationLayer).exitCode

  override def run(args: List[String]): URIO[ZEnv, ExitCode] =
    pp.provideLayer(applicationLayer).exitCode

  val pp: ZIO[
    http4sServer.Http4sServer with tempClient.TempClient with http4sClient.Http4sClient with zio.clock.Clock with zio.console.Console,
    Throwable,
    Unit
  ] = {
    for {
      stream <- tempClient.TempClient.temperatureStream
      _ <- stream.runDrain
      _ <- ZIO.never
    } yield ()
  }

}
