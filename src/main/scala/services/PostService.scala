package services

import model.{CreateReplacePostData, Post}
import persistence.PostRepository

import scala.concurrent.Future

class PostService(repository: PostRepository) extends Service {

  def createPost(data: CreateReplacePostData): Future[Post] = {
    // Build entity
    val postEntity =
      Post(
        "-1", // Id will be automatically assigned by the Repository once saved
        data.title,
        data.tags
      )

    // Save
    repository.save(postEntity)
  }

  def createSeveralPosts(createUpdateData: Seq[CreateReplacePostData]): Future[Seq[Post]] = {
    // Build entities
    val postEntities =
      createUpdateData.map { data =>
        Post(
          "-1", // Id will be automatically assigned by the Repository once saved
          data.title,
          data.tags
        )
      }

    // Save
    repository.saveAll(postEntities: _*)
  }

  def retrievePost(id: String): Future[Option[Post]] =
    repository.find(id)

  def retrievePosts(): Future[Seq[Post]] =
    repository.findAll()

  def retrievePostsByTitle(title: String): Future[Seq[Post]] =
    repository.findByTitle(title)

  def replacePost(id: String, data: CreateReplacePostData): Future[Option[Post]] = {
    def replace(post: Post): Future[Post] = {
      val postEntity = Post(id, data.title, data.tags)
      repository.save(postEntity)
    }

    val result =
      retrievePost(id) flatMap {
        case Some(post) => replace(post).map(Some(_))
        case None       => Future.successful(None)
      }

    result
  }

  def deletePost(id: String): Future[Option[Post]] =
    repository.remove(id)

}
