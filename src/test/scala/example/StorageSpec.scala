package example

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import zio.test.DefaultRunnableSpec
import zio.test._
import zio._
import Assertion._
import example.services.repo.storage
import example.services.db.mongo

// object StorageSpec extends DefaultRunnableSpec {

//   val layer =
//     (configuration.ConfigurationLive.layer >+> mongo.MongoDBConnectionLive.layer >+> storage.TemperatureStorageLive.layer)
//       // we need to map the error to a testFailure since that is what is exptected by the test
//       .mapError(TestFailure.fail)

//   override def spec: ZSpec[Environment, Failure] =
//     suite("StorageSpec with live")(
//       testM("Insert something in mongodb") {
//         for {
//           service <- ZIO.accessM[Has[storage.TemperatureStorage]](_.get.store("blaat"))
//         } yield {
//           assert(true)(equalTo(true))
//         }
//       }
//     ).provideCustomLayer(layer)
// }
