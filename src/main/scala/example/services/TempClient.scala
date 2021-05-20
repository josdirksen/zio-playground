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

/** The temperature client calls a remote webservice ever so often and gives
  * access to a stream of temperatures.
  */
object tempClient {

  /** Dependency to inject in other services
    */
  type TempClient = zio.Has[TempClient.Service]

  object TempClient {

    /** We've just got a very simple service, which provides a single stream.
      */
    trait Service {
      val temperatureStream: ZStream[Clock with Console with http4sClient.Http4sClient, Throwable, String]
    }

    /** The implementation of the service, providing access to the stream. For the creation
      * of this service, we don't have any other dependencies.
      */
    val live: ZLayer[http4sClient.Http4sClient, Throwable, TempClient] =
      ZLayer.succeed {
        new Service {
          override val temperatureStream
              : ZStream[Clock with Console with http4sClient.Http4sClient, Throwable, String] =
            ZStream
              .fromSchedule(Schedule.spaced(Duration.ofSeconds(1L)))
              .mapM { el => ZIO.succeed(el.toString()) }
              .mapM { el => makeCall }
              .tap { p => putStrLn(p) }
        }
      }

    private def makeCall(): ZIO[http4sClient.Http4sClient, Throwable, String] = {
      for {
        client <- ZIO.access[http4sClient.Http4sClient] { el => el.get }
        // for converting to string, we can use the standard EntityDecoder from HTTP4S together with
        // the zio.interop.cats_ for mapping the Cats stuff to ZIO
        res <- client.expect[String](
          "https://www.7timer.info/bin/astro.php?lon=113.2&lat=23.1&ac=0&unit=metric&output=json&tzshift=0"
        )
      } yield (res)
    }

    // get the zstream within the context of the provided environment. This will
    // return a stream that, when drained, will provide a temperature update every
    // tick.
    val temperatureStream: URIO[TempClient, ZStream[
      Clock with Console with http4sClient.Http4sClient,
      Throwable,
      String
    ]] = ZIO.access(_.get.temperatureStream)
  }
}

// https://www.7timer.info/bin/astro.php?lon=113.2&lat=23.1&ac=0&unit=metric&output=json&tzshift=0
