import sangria.ast.Document
import sangria.schema._
import sangria.execution._
import sangria.macros._
import sangria.macros.derive._
import sangria.parser.QueryParser

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

val queryType = ObjectType("query", fields[Unit, Unit](
  Field("hello", StringType, resolve = _ => "Hello World")
))

val schema = Schema(queryType)

val query = graphql"{ hello }"



case class Picture( width: Int,  height: Int, url: Option[String])

val PictureType = ObjectType("Picture", "The product picture",
  fields[Unit, models.Picture](
    Field("width", IntType, resolve = _.value.width),
    Field("height", IntType, resolve = _.value.height),
    Field("url", OptionType(StringType), Some("Picture CDN URL"),resolve = _.value.url))
)

implicit val PictureType = deriveObjectType[Unit, models.Picture](
  ObjectTypeDescription("The product picture"),
  DocumentField("url", "Picture CDN URL")
)

trait Identifiable {
  def id: String
}

val IdentifiableType = InterfaceType(
  "Identifiable",
  "Entity that can be identified",
  fields[Unit, models.Identifiable](
    Field("id", StringType, resolve = _.value.id)))

case class Product(id: String, name: String, description: String) extends models.Identifiable {
  def picture(size: Int): models.Picture = {
    models.Picture(width = size, height = size, url = Some(s"//cdn.com/$size/$id.jpg"))
  }
}

val ProductType = deriveObjectType[Unit, models.Product](
  Interfaces(IdentifiableType),
  IncludeMethods("picture")
)


class ProductRepo {
  private val Products = List(
    models.Product("1", "Cheesecake", "Tasty"),
    models.Product("2", "Health Potion", "+50 HP"))

  def product(id: String): Option[models.Product] =
    Products find (_.id == id)

  def products: List[models.Product] = Products
}

val Id = Argument("id", StringType)

val QueryType = ObjectType("Query", fields[models.ProductRepo, Unit](
  Field("product", OptionType(ProductType),
    description = Some("Returns a product with specific `id`."),
    arguments = Id :: Nil,
    resolve = c => c.ctx.product(c arg Id)),

  Field("products", ListType(ProductType),
    description = Some("Returns a list of all available products."),
    resolve = _.ctx.products))
)

val schema =  Schema(QueryType)

val query =
  """ query MyProduct { product(id: "2") { name description picture(size: 500) { width, height, url } } } """



import sangria.execution._
import sangria.marshalling.circe._

import io.circe.Json


QueryParser.parse(query) match {
  case Success(document: Document) =>
    println(document.renderPretty)
    val result : Future[Json] = Executor.execute(schema, document, new models.ProductRepo)
    result.onComplete(c => c map{ x =>
      println("asdnjkasdnkj")
      println(x)
    })
    result

  case Failure(error) => println(s"Syntax error: ${error.getMessage}")
}






