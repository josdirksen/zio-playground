package example

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import zio.test.DefaultRunnableSpec
import zio.test._
import Assertion._

object LiveConfigLoaderSpec extends DefaultRunnableSpec {
  override def spec: ZSpec[Environment, Failure] =
    suite("ConfigLoaderSpec with live")(
      testM("Load the configuration") {
        for {
          config <- configuration.Configuration.load
        } yield {
          assert(config.apiConfig.port)(equalTo(8081))
        }
      }
    ).provideCustomLayer(configuration.Configuration.live)
}
