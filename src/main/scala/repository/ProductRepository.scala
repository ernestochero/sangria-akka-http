package repository

import org.mongodb.scala._
import models._
import org.bson.types.ObjectId

import scala.concurrent.{ExecutionContext, Future}
class ProductRepository(collection: MongoCollection[Product])(implicit ec:ExecutionContext)  {

  def findById(id: String): Future[Option[Product]] = {
    collection
      .find(Document("_id" -> new ObjectId(id)))
      .first()
      .head()
      .map(Option(_))
  }

  def save(product: Product): Future[String] = {
    collection
      .insertOne(product)
      .head()
      .map { _ => product.id }
  }

  def getAllProducts: Future[List[Product]] = {
    collection.find().toFuture().map(_.toList)
  }

}
