package persistence.common

import com.typesafe.config.Config
import faunadb.FaunaClient
import faunadb.errors.NotFoundException
import faunadb.query.{Class, Obj, _}
import faunadb.values.{Decoder, Encoder, _}
import model.common.{Entity, Id}

import scala.concurrent.{Await, ExecutionContext, Future}

object FaunaRepository {

  trait Implicits {
    implicit object IdCodec extends Decoder[Id] with Encoder[Id] {
      def decode(v: Value, path: FieldPath): Result[Id] = Decoder.StringDecoder.decode(v, path).map(Id(_))
      def encode(t: Id): Value = Encoder.StringEncoder.encode(t.value)
    }

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

trait FaunaRepository[A <: Entity] extends FaunaRepository.Implicits {
  self: Repository[A] =>

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

  override def remove(id: Id): Future[Option[A]] = {
    val result: Future[Value] = client.query(
      Select(
        Value("data"),
        Delete(Ref(Class(className), Value(id.value)))
      )
    )

    result.optDecode
  }

  override def find(id: Id): Future[Option[A]] = {
    val result: Future[Value] =
      client.query(
        Select(Value("data"), Get(Ref(Class(className), Value(id.value))))
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

  def findAll(ids: Id*): Future[Seq[A]] = {
    val result: Future[Value] =
      client.query(
        Map(
          Filter(
            ids.map(_.value),
            Lambda { nextId =>
              Exists(Ref(Class(className), nextId))
            }
          ),
          Lambda(nextId => Select(Value("data"), Get(Ref(Class(className), nextId))))
        )
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

class FaunaSettings(config: Config) {
  private val faunaConfig = config.getConfig("fauna-db")
  val apiKey = faunaConfig.getString("api-key")
}
