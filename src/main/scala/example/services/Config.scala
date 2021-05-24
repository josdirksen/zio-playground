package example

import zio.Task
import zio.Has
import zio.ZIO

import pureconfig.ConfigSource
import pureconfig.generic.auto._
import zio.ZLayer
import zio.TaskLayer
import zio.Layer
import zio.URLayer
import zio.UIO
import zio.URIO
import scala.concurrent.duration.FiniteDuration

// define the domain model used for configuration
case class Config(apiConfig: ApiConfig, temperatureConfig: TemperatureConfig, dbConfig: DBConfig)
case class ApiConfig(endpoint: String, port: Int)
case class TemperatureConfig(endpoint: String, apiKey: String, interval: FiniteDuration)
case class DBConfig(endpoint: String)

// define the configuration dependency to inject into the environment
// and the related functions
object configuration {

  trait Configuration {
    val config: Config
  }

  object Configuration {
    // helper function which access the correct resource from our environment, and lifts
    // it in an effect.
    val config: URIO[Has[Configuration], Config] = ZIO.access(_.get.config)
  }

  /** For this one it isn't useful to create a case class, since we want to make sure
    * the config is correct at the moment we create this layer. So we first load the
    * resources, and create the configuration based on that. That way when we access
    * the
    */
  object ConfigurationLive {
    val layer: TaskLayer[Has[Configuration]] = ZLayer.fromEffect {

      // first we try and load the configuration, if that fails our layer
      // construction fails. If it succeeds, we use it in the
      // new service.
      Task
        .fromEither(
          ConfigSource.default
            .load[Config]
            .left
            .map(pureconfig.error.ConfigReaderException.apply)
        )
        .map { loaded =>
          new Configuration {
            override val config = loaded
          }
        }
    }
  }
}
