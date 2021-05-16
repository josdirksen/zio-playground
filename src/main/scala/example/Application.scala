package example

import zio.ExitCode
import zio.URIO
import zio.ZEnv
import configuration.Configuration
import example.services.http4sServer
import zio.ZIO

/** Base entry point for a ZIO app, which runs the logic within
  * a specified environment
  */
object Application extends zio.App {

  // combine the configuration layer and the standard environment into
  // a new layer.
  val defaultLayer = Configuration.live ++ ZEnv.live
  // the complete application layer also consists out of the http4server, so
  // add that to the default layers list
  val applicationLayer = defaultLayer >+> http4sServer.Http4sServer.live

  // now run the application, by providing it with the application layer
  override def run(args: List[String]): URIO[ZEnv, ExitCode] =
    myAppLogic.provideLayer(applicationLayer).exitCode

  // we can do more stuff here. For now just load the config, cause why not, and wait forever
  val myAppLogic =
    for {
      config <- Configuration.load
      _ <- ZIO.never
    } yield ()

}
