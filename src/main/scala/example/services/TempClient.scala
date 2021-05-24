package example.services

import zio.Has
import zio.stream.ZStream
import zio.ZLayer
import zio.URLayer
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
import example.configuration.Configuration
import org.http4s.client.Client
import zio.UIO
import example.model.Temperature
import cats.instances.double
import example.TemperatureConfig
import scala.jdk.DurationConverters._

/** The temperature client calls a remote webservice ever so often and gives
  * access to a stream of temperatures.
  */
object tempClient {

  trait TempClient {
    val temperatureStream: ZStream[Any, Throwable, Temperature]
  }

  object TempClient {

    /** get the zstream within the context of the provided environment. This will
      * return a stream that, when drained, will provide a temperature update every
      * tick. The only dependency here is the TempClient, retrieving this stream
      * won't result in any errors. Note that we lift the temperatureStream in
      * the UIO, else we can't use the serviceWith function, which expects the
      * result to be A wrapped in a URIO
      */
    val temperatureStream: URIO[Has[TempClient], ZStream[Any, Throwable, Temperature]] =
      ZIO.serviceWith[TempClient](s => UIO.succeed(s.temperatureStream))
  }

  case class TempClientLive(
      client: Client[Task],
      console: Console.Service,
      clock: Clock.Service,
      configuration: Configuration
  ) extends TempClient {

    import org.http4s.circe._
    import io.circe.generic.auto._
    import TempClientLive.OpenWeather._
    implicit val userDecoder = jsonOf[Task, OWResult]

    val tempConfig = configuration.config.temperatureConfig

    override val temperatureStream: ZStream[Any, Throwable, Temperature] =
      // emit one right away and then do the rest in an interval
      (ZStream.succeed(-1L) ++ ZStream.fromSchedule(Schedule.spaced(tempConfig.interval.toJava)))
        .mapM(_ => makeTemperatureCall(tempConfig.endpoint + tempConfig.apiKey))
        .tap { p => console.putStrLn(p.toString()) }
        // We still have to provide the clock for the schedule to eliminate all R
        // and just use everything provided to this service.
        .provide(Has(clock))

    /** Make the call. This requires a client to be in the environment, and returns a string
      */
    private def makeTemperatureCall(url: String): ZIO[Any, Throwable, Temperature] = {
      for {
        // for converting to string, we can use the standard EntityDecoder from HTTP4S together with
        // the zio.interop.cats_ for mapping the Cats stuff to ZIO
        res <- client.expect[OWResult](url)
      } yield (Temperature(res.dt, res.main.temp))
    }
  }

  object TempClientLive {

    object OpenWeather {
      // the openweather model
      case class OWResult(coord: OWCoord, main: OWMain, visibility: Integer, wind: OWWind, dt: Long)
      case class OWCoord(lat: Double, lon: Double)
      case class OWMain(
          temp: Double,
          feels_like: Double,
          temp_min: Double,
          temp_max: Double,
          pressure: Int,
          humidity: Int
      )
      case class OWWind(speed: Double, deg: Long, gust: Double)
    }

    // the dependencies for this service
    type TempClientLiveDeps = Has[Client[Task]]
      with Has[Console.Service]
      with Has[Clock.Service]
      with Has[Configuration]

    // the layer that can be fed to other services, and which specifies what is needed by this layer
    val layer: URLayer[TempClientLiveDeps, Has[TempClient]] = (TempClientLive(_, _, _, _)).toLayer
  }
}
