package persistence

import com.typesafe.config.Config
import faunadb.FaunaClient
import faunadb.errors.NotFoundException
import faunadb.query.{Class, Obj, _}
import faunadb.values.{Decoder, _}

import scala.concurrent.{ExecutionContext, Future}

/**
  * Base trait for implementing Repositories.
  *
  * It defines a set of base methods following DDD's
  * Repository pattern.
  */
trait Repository[A] extends {
  def save(entity: A): Future[A]
  def saveAll(entities: A*): Future[Seq[A]]
  def remove(id: String): Future[Option[A]]
  def find(id: String): Future[Option[A]]
  def findAll(): Future[Seq[A]]
}

trait FaunaRepository[A] extends Repository[A] {

  import FaunaRepository.Implicits._

  // TODO: using Scala's global ExecutionContext for now.
  // Evaluate the use of a a dedicated ExecutionContext for
  // the persistence layer (bulkheading) in a prod environment.
  implicit protected val ec: ExecutionContext = ExecutionContext.global

  protected val client: FaunaClient

  protected val className: String
  protected val classIndexName: String

  implicit protected val codec: Codec[A]

  override def save(entity: A): Future[A] = {
    val result: Future[Value] = client.query(saveQuery(entity))
    result.decode
  }

  override def saveAll(entities: A*): Future[Seq[A]] = {
    val result: Future[Value] = client.query(
      Map(entities, Lambda(nextEntity => saveQuery(nextEntity)))
    )
    result.decode[Seq[A]]
  }

  override def remove(id: String): Future[Option[A]] = {
    val result: Future[Value] = client.query(
      Select(
        Value("data"),
        Delete(Ref(Class(className), Value(id)))
      )
    )

    result.optDecode
  }

  override def find(id: String): Future[Option[A]] = {
    val result: Future[Value] =
      client.query(
        Select(Value("data"), Get(Ref(Class(className), Value(id))))
      )

    result.optDecode
  }

  def findAll(): Future[Seq[A]] = {
    val result =
      client.query(
        Match(Index(Value(classIndexName)))
      )

    result.decode[Seq[A]]
  }

  protected def saveQuery(entity: Expr): Expr =
    Select(
      Value("data"),
      Let(
        Seq(
          "id" ->
            Select(
              Path("ref", "id"),
              Create(Class(Value(className)), Obj("data" -> entity))
            )
        ),
        Update(
          Ref(Class(className), Var("id")),
          Obj("data" -> Obj("id" -> Var("id")))
        )
      )
    )

}

object FaunaRepository {

  object Implicits {

    implicit class ExtendedFutureValue(value: Future[Value]) {
      def decode[A: Decoder](implicit ec: ExecutionContext): Future[A] = value.map(_.to[A].get)

      def optDecode[A: Decoder](implicit ec: ExecutionContext): Future[Option[A]] =
        value
          .decode[A]
          .map(Some(_))
          .recover {
            case _: NotFoundException => None
          }
    }
  }
}

class FaunaSettings(config: Config) {
  private val faunaConfig = config.getConfig("fauna-db")
  val apiKey = faunaConfig.getString("api-key")
}
