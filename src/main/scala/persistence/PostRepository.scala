package persistence

import faunadb.FaunaClient
import faunadb.values.Codec
import model.Post

class PostRepository(faunaClient: FaunaClient) extends FaunaRepository[Post] {
  override protected val client: FaunaClient = faunaClient
  override protected val className: String = "posts"
  override protected val classIndexName: String = "all_posts"
  implicit override protected val codec: Codec[Post] = Codec.Record[Post]
}
