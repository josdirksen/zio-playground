package example.services.db

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

object mongo {

  object MongoDBConnectionLive {

    /** Slightly different approach for when we're using case classes, since we don't really expose
      * the functions on the service, but want to expose a mongoDB connection directly.
      */
    val managedMongoClient: ZManaged[Has[Configuration], Throwable, MongoClient] = for {
      config <- Configuration.config.toManaged_
      mongoClient <- ZManaged.make(acquireConnection(config.dbConfig.endpoint))(releaseConnection(_))
    } yield (mongoClient)

    /** Try and connect the database
      */
    private def acquireConnection(databaseUri: String): Task[MongoClient] = ZIO.fromTry(Try {
      MongoClient(databaseUri)
    })

    /** Release the connection. If an error occurs during releasing, we just ignore it
      * for now. It would probably be better to check the error, whether it can be ignored
      * and log some stuff. But for now this should be enough
      *
      * @param mongoClient the client for which we want to release the connection
      * @return effect that will always succeed
      */
    private def releaseConnection(mongoClient: MongoClient): ZIO[Any, Nothing, Unit] =
      ZIO.fromTry(Try(mongoClient.close)).orElse(ZIO.succeed())

    /** For this component, we're not going to create a case class, since the construction of this layer
      * can fail, and we've got a managed resource for which we want to create a connection.
      */
    val layer: ZLayer[Has[Configuration], Throwable, Has[MongoClient]] = ZLayer.fromManaged(managedMongoClient)
  }

}
