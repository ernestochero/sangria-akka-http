import java.util.concurrent.TimeUnit

import models._
import sangria.execution.deferred.{Fetcher, HasId}
import sangria.macros.derive.{IncludeMethods, Interfaces, deriveObjectType}
import sangria.schema._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

/**
 * Defines a GraphQL schema for the current project
 */
object SchemaDefinition {
  /**
    * Resolves the lists of characters. These resolutions are batched and
    * cached for the duration of a query.
    */
  val characters = Fetcher.caching(
    (ctx: CharacterRepo, ids: Seq[String]) ⇒
      Future.successful(ids.flatMap(id ⇒ ctx.getHuman(id) orElse ctx.getDroid(id))))(HasId(_.id))

  val EpisodeEnum = EnumType(
    "Episode",
    Some("One of the films in the Star Wars Trilogy"),
    List(
      EnumValue("NEWHOPE",
        value = Episode.NEWHOPE,
        description = Some("Released in 1977.")),
      EnumValue("EMPIRE",
        value = Episode.EMPIRE,
        description = Some("Released in 1980.")),
      EnumValue("JEDI",
        value = Episode.JEDI,
        description = Some("Released in 1983."))))

  val Character: InterfaceType[CharacterRepo, models.Character] =
    InterfaceType(
      "Character",
      "A character in the Star Wars Trilogy",
      () ⇒ fields[CharacterRepo, models.Character](
        Field("id", StringType,
          Some("The id of the character."),
          resolve = _.value.id),
        Field("name", OptionType(StringType),
          Some("The name of the character."),
          resolve = _.value.name),
        Field("friends", ListType(Character),
          Some("The friends of the character, or an empty list if they have none."),
          resolve = ctx ⇒ characters.deferSeqOpt(ctx.value.friends)),
        Field("appearsIn", OptionType(ListType(OptionType(EpisodeEnum))),
          Some("Which movies they appear in."),
          resolve = _.value.appearsIn map (e ⇒ Some(e)))
      ))

  val Human =
    ObjectType(
      "Human",
      "A humanoid creature in the Star Wars universe.",
      interfaces[CharacterRepo, Human](Character),
      fields[CharacterRepo, Human](
        Field("id", StringType,
          Some("The id of the human."),
          resolve = _.value.id),
        Field("name", OptionType(StringType),
          Some("The name of the human."),
          resolve = _.value.name),
        Field("friends", ListType(Character),
          Some("The friends of the human, or an empty list if they have none."),
          resolve = ctx ⇒ characters.deferSeqOpt(ctx.value.friends)),
        Field("appearsIn", OptionType(ListType(OptionType(EpisodeEnum))),
          Some("Which movies they appear in."),
          resolve = _.value.appearsIn map (e ⇒ Some(e))),
        Field("homePlanet", OptionType(StringType),
          Some("The home planet of the human, or null if unknown."),
          resolve = _.value.homePlanet)
      ))

  val Droid = ObjectType(
    "Droid",
    "A mechanical creature in the Star Wars universe.",
    interfaces[CharacterRepo, Droid](Character),
    fields[CharacterRepo, Droid](
      Field("id", StringType,
        Some("The id of the droid."),
        resolve = _.value.id),
      Field("name", OptionType(StringType),
        Some("The name of the droid."),
        resolve = ctx ⇒ Future.successful(ctx.value.name)),
      Field("friends", ListType(Character),
        Some("The friends of the droid, or an empty list if they have none."),
        resolve = ctx ⇒ characters.deferSeqOpt(ctx.value.friends)),
      Field("appearsIn", OptionType(ListType(OptionType(EpisodeEnum))),
        Some("Which movies they appear in."),
        resolve = _.value.appearsIn map (e ⇒ Some(e))),
      Field("primaryFunction", OptionType(StringType),
        Some("The primary function of the droid."),
        resolve = _.value.primaryFunction)
    ))

  val ID = Argument("id", StringType, description = "id of the character")

  val EpisodeArg = Argument("episode", OptionInputType(EpisodeEnum),
    description = "If omitted, returns the hero of the whole saga. If provided, returns the hero of that particular episode.")

  val LimitArg = Argument("limit", OptionInputType(IntType), defaultValue = 20)
  val OffsetArg = Argument("offset", OptionInputType(IntType), defaultValue = 0)

  val Query = ObjectType(
    "Query", fields[CharacterRepo, Unit](
      Field("hero", Character,
        arguments = EpisodeArg :: Nil,
        deprecationReason = Some("Use `human` or `droid` fields instead"),
        resolve = (ctx) ⇒ ctx.ctx.getHero(ctx.arg(EpisodeArg))),
      Field("human", OptionType(Human),
        arguments = ID :: Nil,
        resolve = ctx ⇒ ctx.ctx.getHuman(ctx arg ID)),
      Field("droid", Droid,
        arguments = ID :: Nil,
        resolve = ctx ⇒ ctx.ctx.getDroid(ctx arg ID).get),
      Field("humans", ListType(Human),
        arguments = LimitArg :: OffsetArg :: Nil,
        resolve = ctx ⇒ ctx.ctx.getHumans(ctx arg LimitArg, ctx arg OffsetArg)),
      Field("droids", ListType(Droid),
        arguments = LimitArg :: OffsetArg :: Nil,
        resolve = ctx ⇒ ctx.ctx.getDroids(ctx arg LimitArg, ctx arg OffsetArg))
    ))

  val StarWarsSchema = Schema(Query)
}

object SchemaDefinition3 {

  /* implicit val PictureType =
     deriveObjectType[Unit, Picture](
       ObjectTypeDescription("The product picture"),
       DocumentField("url", "Picture CDN URL"))*/
  val products = Fetcher.caching(
    (ctx: ProductRepo, ids: Seq[String]) => { ctx.products(ids)})(HasId(c=> c.id))

  implicit val PictureType = ObjectType("Picture", "The product picture",
    fields[Unit, Picture](
      Field("width", IntType, resolve = _.value.width),
      Field("height", IntType, resolve = _.value.height),
      Field("url", OptionType(StringType), Some("Picture CDN URL"),resolve = _.value.url))
  )

  val IdentifiableType = InterfaceType(
    "Identifiable",
    "Entity that can be identified",
    fields[Unit, Identifiable](
      Field("id", StringType, resolve = _.value.id)))

  implicit val ProductType = deriveObjectType[Unit, Product](
    Interfaces(IdentifiableType),
    IncludeMethods("picture"))

  val Id = Argument("id", StringType)
  val NameArg = Argument("name", StringType)
  val DescriptionArg = Argument("description", StringType)
  val Ids = Argument("ids", ListInputType(StringType))

  val QueryType = ObjectType("Query", fields[ProductRepo, Unit](
    Field("allProducts", ListType(ProductType),
      description = Some("Returns a list of all available products."),
      resolve = _.ctx.allProducts),

    Field("product", OptionType(ProductType),
      description = Some("Returns a product with specific `id`."),
      arguments = Id :: Nil,
      resolve = c => c.ctx.product(c arg Id)),

    Field("products", ListType(ProductType),
      description = Some("Returns a list of all available products."),
      arguments = Ids :: Nil,
      resolve = c => c.ctx.products(c.arg[List[String]]("ids")))
   )
  )

  val MutationType = ObjectType("Mutation", fields[ProductRepo, Unit](
    Field("addProduct", ProductType,
      arguments = NameArg :: DescriptionArg :: Nil,
      resolve = c => c.ctx.saveProduct(c.arg(NameArg), c.arg(DescriptionArg))
    )
  ))

  val ProductSchema = Schema(QueryType, Some(MutationType))
}