package example.services

import example.api.Routes
import example.configuration.Configuration
import org.http4s.HttpRoutes
import org.http4s.server.Server
import org.http4s.server.blaze.BlazeServerBuilder
import zio._

// we need implicit conversions here to map the runtime provided by ZIO to the times and concurrent
// stuff needed by cats
import zio.interop.catz.implicits._
import zio.interop.catz._

/** Service providing access to a nested http4s server. This follows the same structure
  * of all the other services, but instead of returning a specific trait, we wrap the
  * `Server` from org.https4s
  */
object http4sServer {
  type Logger = zio.Has[Http4sServer.Service]

  object Http4sServer {

    type Service = Has[Server]

    val live: ZLayer[ZEnv with Configuration, Throwable, Service] =
      ZLayer.fromManaged {
        for {
          // we should probably cache this, and make this a function, where we
          // pass in the relevant configuration
          config <- Configuration.load.toManaged_

          // the implicit runtime provided the implicits needed to create the Timer and ConcurrentEffect
          // typeclasses. This is done by the zio / cats interop imports
          server <- ZManaged.runtime[ZEnv].flatMap {
            implicit runtime: Runtime[ZEnv] =>
              BlazeServerBuilder[Task](runtime.platform.executor.asEC)
                .bindHttp(config.apiConfig.port, config.apiConfig.endpoint)
                .withHttpApp(Routes.routes)
                .resource
                .toManagedZIO
          }
        } yield (server)
      }
  }
}
