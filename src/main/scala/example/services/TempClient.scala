package example.services

import zio.Has
import zio.stream.ZStream
import zio.ZLayer
import zio.Layer
import zio.Schedule
import java.time.Duration
import zio.clock.Clock
import zio.console._
import zio.console.Console
import zio.ZIO
import zio.URIO
import javax.xml.transform.Templates

// we need the interop stuff since http only provides default
// entity decoders for cats
import zio.interop.catz._
import org.http4s.EntityDecoder
import zio.Task
import example.configuration

/** The temperature client calls a remote webservice ever so often and gives
  * access to a stream of temperatures.
  */
object tempClient {

  /** Dependency to inject in other services
    */
  type TempClient = zio.Has[TempClient.Service]

  object TempClient {

    //We've just got a very simple service, which provides a single stream.
    trait Service {
      val temperatureStream: ZStream[Any, Throwable, String]
    }

    // What do we need in the construction of this service.
    type ServiceDeps = http4sClient.Http4sClient with Console with Clock with configuration.Configuration

    /** The implementation of the service, providing access to the stream. For the creation
      * of this service, we already pass in a Http4sClient, which we use in the closure where
      * we create the stream.
      */
    val live: ZLayer[ServiceDeps, Throwable, TempClient] =
      ZLayer.fromFunction { env =>
        new Service {

          /** We create a zstream. This stream has operators which require specific
            * information in the environment. Since we already got these as dependencies
            * when creating this layer, we use provide(env) to inject these dependencies, so
            * they are removed from the R of the resulting ZStream.
            */
          override val temperatureStream: ZStream[Any, Throwable, String] =
            ZStream
              .fromSchedule(Schedule.spaced(Duration.ofSeconds(1L)))
              .mapM { el =>
                for {
                  // we should not need to load the configuration each time, but load
                  // it on module/service initiation instead.
                  config <- configuration.Configuration.load
                  res <- makeTemperatureCall(config.temperatureConfig.endpoint)
                } yield (res)
              }
              .tap { p => putStrLn(p) }
              .provide(env)
        }
      }

    /** Make the call. This requires a client to be in the environment, and returns a string
      */
    private def makeTemperatureCall(url: String): ZIO[http4sClient.Http4sClient, Throwable, String] = {
      for {
        // for converting to string, we can use the standard EntityDecoder from HTTP4S together with
        // the zio.interop.cats_ for mapping the Cats stuff to ZIO
        client <- ZIO.access[http4sClient.Http4sClient](_.get)
        res <- client.expect[String](url)
      } yield (res)
    }

    /** get the zstream within the context of the provided environment. This will
      * return a stream that, when drained, will provide a temperature update every
      * tick. The only dependency here is the TempClient, retrieving this stream
      * won't result in any errors
      */
    val temperatureStream: URIO[TempClient, ZStream[Any, Throwable, String]] = ZIO.access(_.get.temperatureStream)
  }
}
