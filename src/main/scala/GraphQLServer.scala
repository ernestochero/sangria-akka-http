import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import sangria.ast.Document
import sangria.execution.{ErrorWithResolver, Executor, QueryAnalysisError}
import sangria.marshalling.sprayJson._
import sangria.parser.QueryParser
import spray.json.{JsObject, JsString, JsValue}

import mongodb.Mongo
import repository.ProductRepository

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global

object GraphQLServer {

  val repository = new ProductRepository(Mongo.productsCollection)

  def enpoint(requestJson: JsValue)(implicit ec: ExecutionContext): Route = {

    val JsObject(fields) = requestJson
    val JsString(query) = fields("query")

    val operation = fields.get("operationName") collect {
      case JsString(op) => op
    }

    val vars = fields.get("variables") match {
      case Some(obj: JsObject) => obj
      case _ => JsObject.empty
    }

    QueryParser.parse(query) match {
      case Success(queryAst) =>
        complete(executeGraphQLQuery(queryAst, operation, vars))
      case Failure(error) =>
        complete(BadRequest, JsObject("error" -> JsString(error.getMessage)))


    }
  }


  private def executeGraphQLQuery(query:Document, op: Option[String], vars: JsObject)(implicit ec: ExecutionContext) = {
    Executor.execute(SchemaDefinition3.ProductSchema, query,  repository, operationName = op, variables = vars)
      .map(OK -> _)
      .recover {
        case error: QueryAnalysisError => BadRequest -> error.resolveError
        case error: ErrorWithResolver => InternalServerError -> error.resolveError
      }
  }

}
