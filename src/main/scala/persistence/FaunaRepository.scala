package persistence

import com.typesafe.config.Config
import faunadb.FaunaClient
import faunadb.errors.NotFoundException
import faunadb.query.{Class, Obj, _}
import faunadb.values.{Decoder, _}
import model.Entity

import scala.concurrent.{ExecutionContext, Future}

trait FaunaRepository[A <: Entity] extends Repository[A] with FaunaRepository.Implicits {

  // Note: using Scala's global ExecutionContext for now.
  // Evaluate the use of a a dedicated ExecutionContext for
  // the persistence layer (bulkheading) in a prod environment.
  implicit protected val ec: ExecutionContext = ExecutionContext.global

  protected val client: FaunaClient

  protected val className: String
  protected val classIndexName: String

  implicit protected val codec: Codec[A]

  override def save(entity: A): Future[A] = {
    val result: Future[Value] = client.query(saveQuery(entity.id, entity))
    result.decode
  }

  override def saveAll(entities: A*): Future[Seq[A]] = {
    val result: Future[Value] = client.query(
      Map(
        entities,
        Lambda(nextEntity => saveQuery(Select(Value("id"), nextEntity), nextEntity))
      )
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
    val result: Future[Value] =
      client.query(
        Map(
          SelectAll(
            Path("data", "id"),
            Paginate(
              Match(Index(Value(classIndexName)))
            )
          ),
          Lambda(nextId => Select(Value("data"), Get(Ref(Class(className), nextId))))
        )
      )

    result.decode[Seq[A]]
  }

  protected def saveQuery(id: Expr, data: Expr): Expr =
    Select(
      Value("data"),
      If(
        Exists(Ref(Class(className), id)),
        replaceQuery(id, data),
        createQuery(data)
      )
    )

  protected def createQuery(data: Expr): Expr =
    Let(
      Seq(
        "id" ->
          Select(
            Path("ref", "id"),
            Create(Class(Value(className)), Obj("data" -> data))
          )
      ),
      Update(
        Ref(Class(className), Var("id")),
        Obj("data" -> Obj("id" -> Var("id")))
      )
    )

  protected def replaceQuery(id: Expr, data: Expr): Expr =
    Replace(Ref(Class(className), id), Obj("data" -> data))

}

object FaunaRepository {

  trait Implicits {

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

  object Implicits extends Implicits
}

// Settings
class FaunaSettings(config: Config) {
  private val faunaConfig = config.getConfig("fauna-db")
  val endpoint = faunaConfig.getString("endpoint")
  val secret = faunaConfig.getString("secret")
}
