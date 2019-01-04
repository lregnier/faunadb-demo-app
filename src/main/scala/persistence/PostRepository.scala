package persistence

import faunadb.FaunaClient
import faunadb.query.{Map, _}
import faunadb.values.{Codec, Value}
import model.Post

import scala.concurrent.Future

class PostRepository(faunaClient: FaunaClient) extends FaunaRepository[Post] {
  override protected val client: FaunaClient = faunaClient
  override protected val className: String = "posts"
  override protected val classIndexName: String = "all_posts"
  implicit override protected val codec: Codec[Post] = Codec.Record[Post]

  //-- Custom repository operations specific to the current entity go below --//

  def findByTitle(title: String): Future[Seq[Post]] = {
    val result: Future[Value] =
      client.query(
        Map(
          SelectAll(
            Path("data", "id"),
            Paginate(
              Match(Index("posts_by_title"), title)
            )
          ),
          Lambda(nextId => Select(Value("data"), Get(Ref(Class(className), nextId))))
        )
      )

    result.decode[Seq[Post]]

  }

}
