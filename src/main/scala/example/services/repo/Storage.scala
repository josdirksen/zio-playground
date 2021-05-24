package example.services.repo

import zio.UIO
import example.configuration.Configuration
import zio.URLayer
import zio.Has
import zio._

import org.mongodb.scala._
import org.mongodb.scala.bson.codecs.DocumentCodecProvider
import scala.collection.JavaConverters._
import com.typesafe.config.Config
import scala.util.Try

import org.mongodb.scala.bson.codecs.Macros
import org.mongodb.scala.MongoClient.DEFAULT_CODEC_REGISTRY
import org.bson.codecs.configuration.CodecRegistries.{fromRegistries, fromProviders}
import scala.reflect.ClassTag
import java.util.UUID

import zio.interop.reactivestreams._
import example.model.Temperature

object storage {

  /** The trait for storing.
    */
  trait TemperatureStorage {
    def insert(temperature: Temperature): ZIO[Any, Throwable, Unit]
    def getAll(): ZIO[Any, Throwable, List[Temperature]]
  }

  /** The implementation of temp storage, very naive for now, just to show the different parts connected to one another
    *
    * @param configuration
    */
  case class TemperatureStorageLive(configuration: Configuration, mongoClient: MongoClient) extends TemperatureStorage {

    val temperatureCodecProvider = Macros.createCodecProvider[Temperature]()
    val codecRegistry = fromRegistries(fromProviders(temperatureCodecProvider), DEFAULT_CODEC_REGISTRY)

    /** Try and store the temperature string
      * @param temp element to store
      * @return ZIO containing the result
      */
    override def insert(temperature: Temperature): ZIO[Any, Throwable, Unit] = withCollection[Unit, Temperature] {
      collection =>
        collection
          // insert the entry
          .insertOne(temperature)
          // convert to a single result, since the result is a singleObservable
          .toStream()
          .runHead
          // we should do some more error handling here in a real world scenario
          .flatMap {
            case Some(res) => ZIO.succeed()
            case None      => ZIO.fail(new IllegalArgumentException("Expected result from mongodb"))
          }
    }

    /** Return all the elements we currently have
      *
      * @return list of all the temperatures we've got stored
      */
    override def getAll(): ZIO[Any, Throwable, List[Temperature]] = withCollection[List[Temperature], Temperature] {
      _.find()
        .toStream()
        .runCollect
        .map(_.toList)
    }

    /** Get the database and collection to which to store.
      *
      * @param f function to call within the context of this collection
      * @return result of wrapped
      */
    private def withCollection[A, T: ClassTag](
        f: MongoCollection[T] => ZIO[Any, Throwable, A]
    ): ZIO[Any, Throwable, A] = {
      val collectionZIO = ZIO.fromTry(Try {
        // TODO: we should get the database at least from the configuration
        mongoClient.getDatabase("sampleservice").withCodecRegistry(codecRegistry).getCollection[T]("temperatures")
      })
      collectionZIO.flatMap(f)
    }
  }

  object TemperatureStorageLive {
    val layer: URLayer[Has[Configuration] with Has[MongoClient], Has[TemperatureStorage]] =
      (TemperatureStorageLive(_, _)).toLayer
  }

}
