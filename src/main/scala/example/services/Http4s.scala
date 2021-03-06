package example.services

import example.api.Routes
import example.api.Routes.TemperatureRoute
import example.configuration.Configuration
import org.http4s.HttpRoutes
import org.http4s.server.Server
import org.http4s.client.Client
import org.http4s.server.blaze.BlazeServerBuilder
import zio._

import org.http4s.implicits._

// we need implicit conversions here to map the runtime provided by ZIO to the times and concurrent
// stuff needed by cats
import zio.interop.catz.implicits._
import zio.interop.catz._
import example.Config
import org.http4s.client.blaze.BlazeClientBuilder

/** Service providing access to a nested http4s server. This follows the same structure
  * of all the other services, but instead of returning a specific trait, we wrap the
  * `Server` from org.https4s
  */
object http4sServer {

  object Http4sServerLive {

    val layer: ZLayer[ZEnv with Has[Configuration] with Has[TemperatureRoute], Throwable, Has[Server]] =
      ZLayer.fromManaged {
        for {
          // we should probably cache this, and make this a function, where we
          // pass in the relevant configuration
          config <- Configuration.config.toManaged_
          routes <- TemperatureRoute.temperatureRoutes.toManaged_
          // the implicit runtime provided the implicits needed to create the Timer and ConcurrentEffect
          // typeclasses. This is done by the zio / cats interop imports
          server <- ZManaged.runtime[ZEnv].flatMap { implicit runtime: Runtime[ZEnv] =>
            BlazeServerBuilder[Task](runtime.platform.executor.asEC)
              .bindHttp(config.apiConfig.port, config.apiConfig.endpoint)
              // TODO: Inject the routes object
              .withHttpApp(routes.orNotFound)
              .resource
              .toManagedZIO
          }
        } yield (server)
      }
  }
}

object http4sClient {

  object Http4sClientLive {
    val layer: ZLayer[Any, Throwable, Has[Client[Task]]] = {
      implicit val runtime: Runtime[ZEnv] = Runtime.default
      val res = BlazeClientBuilder[Task](runtime.platform.executor.asEC).resource.toManagedZIO
      ZLayer.fromManaged(res)
    }
  }
}
