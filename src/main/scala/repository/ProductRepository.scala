package repository

import org.mongodb.scala._
import models._
import org.bson.types.ObjectId

import scala.concurrent.{ExecutionContext, Future}
class ProductRepository(collection: MongoCollection[ProductDomain])(implicit ec:ExecutionContext)  {

  def findById(id: String): Future[Option[ProductDomain]] = {
    collection
      .find(Document("_id" -> new ObjectId(id)))
      .first()
      .head()
      .map(Option(_))
  }

  def save(product: ProductDomain) = {
    collection
      .insertOne(product)
      .head()
      .map { _ => product }
  }

  def getAllProducts: Future[Seq[ProductDomain]] = { collection.find().toFuture() }

}
