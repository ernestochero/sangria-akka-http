package mongodb

import com.typesafe.config.ConfigFactory
import models.{ProductDomain}
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.mongodb.scala.bson.codecs.{DEFAULT_CODEC_REGISTRY, Macros}
import org.mongodb.scala.{MongoClient, MongoCollection, MongoDatabase}

object Mongo {
  lazy val userCodecProvider = Macros.createCodecProviderIgnoreNone[ProductDomain]()
  lazy val config = ConfigFactory.load()
  lazy val mongoClient: MongoClient = MongoClient(config.getString("mongo.uri"))
  lazy val codecRegistry = fromRegistries(fromProviders(userCodecProvider), DEFAULT_CODEC_REGISTRY)
  lazy val database: MongoDatabase = mongoClient.getDatabase(config.getString("mongo.database")).withCodecRegistry(codecRegistry)

  lazy val productsCollection: MongoCollection[ProductDomain] = database.getCollection[ProductDomain]("products")
}
