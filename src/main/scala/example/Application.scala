package example

import zio.ExitCode
import zio.URIO
import zio.ZEnv
import configuration.ConfigurationLive
import example.services.http4sServer
import zio.ZIO
import example.services.tempClient
import zio.stream.ZSink
import example.services.http4sClient
import example.api.Routes.TemperatureRouteLive
import zio.Has
import org.http4s.server.Server
import org.http4s.client.Client
import zio.Task
import example.services.repo.storage
import example.services.db

/** Base entry point for a ZIO app, which runs the logic within
  * a specified environment
  */
object Application extends zio.App {

  // combine the configuration layer and the standard environment into
  // a new layer.
  val defaultLayer = ConfigurationLive.layer ++ ZEnv.live

  // the storage layer
  val storageLayer = defaultLayer >+> db.mongo.MongoDBConnectionLive.layer >>> storage.TemperatureStorageLive.layer
  val httpsLayer =
    storageLayer >+> TemperatureRouteLive.layer ++ defaultLayer >+> http4sServer.Http4sServerLive.layer ++ http4sClient.Http4sClientLive.layer

  // combine the layers into a single layer to feed into the program
  val applicationLayer =
    httpsLayer >+> tempClient.TempClientLive.layer ++ storageLayer

  /** Provide the layer to the program and run it
    *
    * @param args
    * @return
    */
  override def run(args: List[String]): URIO[ZEnv, ExitCode] =
    pp.provideLayer(applicationLayer).exitCode

  /** Helper type to indicate which dependencies are needed to run the program
    */
  type ProgramDeps = Has[Server]
    with Has[storage.TemperatureStorage]
    with Has[tempClient.TempClient]
    with Has[Client[Task]]
    with zio.clock.Clock
    with zio.console.Console

  /** The program that starts polling the https endpoint and write the results to
    * the database.
    */
  val pp: ZIO[ProgramDeps, Throwable, Unit] = {
    for {
      // start the stream, which polls the temperature endpoint
      stream <- tempClient.TempClient.temperatureStream
      mapped = stream.mapM(temperatureString =>
        ZIO.serviceWith[storage.TemperatureStorage](_.insert(temperatureString))
      )
      _ <- mapped.runDrain
      _ <- ZIO.never
    } yield ()
  }

}
