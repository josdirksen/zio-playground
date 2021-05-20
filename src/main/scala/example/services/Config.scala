package example

import zio.Task
import zio.Has
import zio.ZIO

import pureconfig.ConfigSource
import pureconfig.generic.auto._
import zio.ZLayer
import zio.Layer

// define the domain model used for configuration
case class Config(apiConfig: ApiConfig, temperatureConfig: TemperatureConfig)
case class ApiConfig(endpoint: String, port: Int)
case class TemperatureConfig(endpoint: String)

// define the configuration dependency to inject into the environment
// and the related functions
object configuration {

  // Custom type to inject
  type Configuration = zio.Has[Configuration.Service]

  // contains the service definition, and multiple implementations
  // based on the environment we want to run in.
  object Configuration {

    // we just have the load function inside the service to load
    // some configuration
    trait Service {
      val load: Task[Config]
    }

    // we can have multiple implementations of this service. This is the
    // live implementation, which loads the config through pureconfig
    val live: Layer[Nothing, Configuration] = ZLayer.succeed {
      new Service {
        override val load: Task[Config] = Task.fromEither(
          ConfigSource.default
            .load[Config]
            .left
            .map(pureconfig.error.ConfigReaderException.apply)
        )
      }
    }

    // helper function which access the correct resource from our environment.
    val load: ZIO[Configuration, Throwable, Config] = ZIO.accessM(_.get.load)
  }
}
