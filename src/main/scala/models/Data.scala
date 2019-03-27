package models
import java.util.concurrent.TimeUnit

import akka.util.Timeout
import repository._
import mongodb._
import org.bson.types.ObjectId
import org.mongodb.scala.MongoCollection

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}
import scala.util.{Failure, Success}
object Episode extends Enumeration {
  val NEWHOPE, EMPIRE, JEDI = Value
}

trait Character {
  def id: String
  def name: Option[String]
  def friends: List[String]
  def appearsIn: List[Episode.Value]
}

case class Human(
  id: String,
  name: Option[String],
  friends: List[String],
  appearsIn: List[Episode.Value],
  homePlanet: Option[String]) extends Character

case class Droid(
  id: String,
  name: Option[String],
  friends: List[String],
  appearsIn: List[Episode.Value],
  primaryFunction: Option[String]) extends Character

class CharacterRepo {
  import CharacterRepo._

  def getHero(episode: Option[Episode.Value]) =
    episode flatMap (_ ⇒ getHuman("1000")) getOrElse droids.last

  def getHuman(id: String): Option[Human] = humans.find(c ⇒ c.id == id)

  def getDroid(id: String): Option[Droid] = droids.find(c ⇒ c.id == id)

  def getHumans(limit: Int, offset: Int): List[Human] = humans.drop(offset).take(limit)

  def getDroids(limit: Int, offset: Int): List[Droid] = droids.drop(offset).take(limit)
}

object CharacterRepo {
  val humans = List(
    Human(
      id = "1000",
      name = Some("Luke Skywalker"),
      friends = List("1002", "1003", "2000", "2001"),
      appearsIn = List(Episode.NEWHOPE, Episode.EMPIRE, Episode.JEDI),
      homePlanet = Some("Tatooine")),
    Human(
      id = "1001",
      name = Some("Darth Vader"),
      friends = List("1004"),
      appearsIn = List(Episode.NEWHOPE, Episode.EMPIRE, Episode.JEDI),
      homePlanet = Some("Tatooine")),
    Human(
      id = "1002",
      name = Some("Han Solo"),
      friends = List("1000", "1003", "2001"),
      appearsIn = List(Episode.NEWHOPE, Episode.EMPIRE, Episode.JEDI),
      homePlanet = None),
    Human(
      id = "1003",
      name = Some("Leia Organa"),
      friends = List("1000", "1002", "2000", "2001"),
      appearsIn = List(Episode.NEWHOPE, Episode.EMPIRE, Episode.JEDI),
      homePlanet = Some("Alderaan")),
    Human(
      id = "1004",
      name = Some("Wilhuff Tarkin"),
      friends = List("1001"),
      appearsIn = List(Episode.NEWHOPE, Episode.EMPIRE, Episode.JEDI),
      homePlanet = None)
  )

  val droids = List(
    Droid(
      id = "2000",
      name = Some("C-3PO"),
      friends = List("1000", "1002", "1003", "2001"),
      appearsIn = List(Episode.NEWHOPE, Episode.EMPIRE, Episode.JEDI),
      primaryFunction = Some("Protocol")),
    Droid(
      id = "2001",
      name = Some("R2-D2"),
      friends = List("1000", "1002", "1003"),
      appearsIn = List(Episode.NEWHOPE, Episode.EMPIRE, Episode.JEDI),
      primaryFunction = Some("Astromech"))
  )
}


trait Identifiable {
  def id: String
}
case class Picture( width: Int,  height: Int, url: Option[String])


case class ProductDomain(_id: ObjectId, name: String, description: String) {
  def asResource = Product(_id.toHexString, name, description)
}

case class Product(id: String, name: String, description: String) extends Identifiable {
  def picture(size: Int): Picture = {
    Picture(width = size, height = size, url = Some(s"//cdn.com/$size/$id.jpg"))
  }

  def asDomain = ProductDomain(if (id == null) ObjectId.get() else new ObjectId(id), name , description)

}


class StorageRedisCache {

}

class ProductRepo(repository: ProductRepository)(implicit ec:ExecutionContext) {

  def  storeProductsOnCache(products:List[Product]) = {
    products.foreach(println(_))
    products
  }

  def results = Await.result(repository.getAllProducts.map(_.map(_.asResource)), Duration(10, TimeUnit.SECONDS))

  def getProducts = repository.getAllProducts

  private val Products = List(
    Product("5c798c2137024ab47a2b9617", "Cheesecake", "Tasty"),
    Product("5c798c2137024ab47a2b9618", "Health Potion", "+50 HP")
    ) ++ results

  def product(id: String): Option[Product] =
    Products find (_.id == id)

  def products: List[Product] = Products
}
