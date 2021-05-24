package example.api

import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.Server
import zio.Task
import zio.interop.catz._
import zio.ZLayer
import zio.Has
import example.services.repo.storage
import example.model.Temperature
import zio.ZIO
import zio.URIO

/** We can define the routes themselves as a layer and inject the dependent services in the
  * HTTP server. Or we can inject the service directly here, and inject the routes in the
  * HTTP4S server.
  *
  * The first approach might be easier, but the second one seems to be more clean.
  */
object Routes {

  trait TemperatureRoute {
    val routes: HttpRoutes[Task]
  }

  object TemperatureRoute {
    // helper function which access the correct resource from our environment, and lifts
    // it in an effect.
    val temperatureRoutes: URIO[Has[TemperatureRoute], HttpRoutes[Task]] = ZIO.access(_.get.routes)
  }

  object TemperatureRouteLive {

    private val dsl = Http4sDsl[Task]
    import dsl._
    import io.circe.generic.auto._, io.circe.syntax._
    import org.http4s.dsl.Http4sDsl
    import org.http4s.circe._
    import zio.interop.catz._

    /** A simple layer which returns the routes for the temperature.
      */
    val layer: ZLayer[Has[storage.TemperatureStorage], Nothing, Has[TemperatureRoute]] =
      ZLayer.fromService { storage =>
        new TemperatureRoute {

          val routes = HttpRoutes
            .of[Task] {
              case GET -> Root / "temperatures" => {
                storage.getAll().flatMap { all => Ok(all.asJson) }
              }
              case GET -> Root / "users" / IntVar(id) => {
                Created("A value here")
              }
              case POST -> Root / "users" => {
                Created("And another one here")
              }
            }
        }
      }

  }

}
