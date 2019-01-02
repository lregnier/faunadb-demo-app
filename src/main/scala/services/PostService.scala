package services

import model.{CreateReplacePostData, Post, UpdatePostData}
import persistence.PostRepository

import scala.concurrent.{ExecutionContext, Future}

/**
  * Base trait for implementing Domain Services
  */
trait Service {
  // Note: using Scala's global ExecutionContext for now.
  // Evaluate the use of a dedicated ExecutionContext for
  // the services layer (bulkheading) in a prod environment.
  implicit protected val ec: ExecutionContext = ExecutionContext.global
}

class PostService(repository: PostRepository) extends Service {

  def createPost(createUpdateData: CreateReplacePostData): Future[Post] = {
    val postEntity =
      Post(
        "", // Id will be automatically assigned by the Repository once saved
        createUpdateData.title,
        createUpdateData.tags
      )
    repository.save(postEntity)
  }

  def createSeveralPosts(createUpdateData: Seq[CreateReplacePostData]): Future[Seq[Post]] = {
    val postEntities =
      createUpdateData.map { data =>
        Post(
          "", // Id will be automatically assigned by the Repository once saved
          data.title,
          data.tags
        )
      }

    repository.saveAll(postEntities: _*)
  }

  def retrievePost(id: String): Future[Option[Post]] =
    repository.find(id)

  def retrievePosts(): Future[Seq[Post]] =
    repository.findAll()

  def retrievePostsByTags(tags: Seq[String]): Future[Seq[Post]] = ???

  def updatePost(id: String, data: UpdatePostData): Future[Option[Post]] = {
    def update(post: Post): Future[Post] = {
      val updatedTitle = data.title.getOrElse(post.title)
      val updatedTags = data.tags.getOrElse(post.tags)
      repository.save(post.copy(title = updatedTitle, tags = updatedTags))
    }

    // TODO: move query to repository using FQL
    val result =
      retrievePost(id) flatMap {
        case Some(post) => update(post).map(Some(_))
        case None       => Future.successful(None)
      }

    result
  }

  def replacePost(id: String, data: CreateReplacePostData): Future[Option[Post]] = {
    def update(post: Post): Future[Post] =
      repository.save(post.copy(title = data.title, tags = data.tags))

    // TODO: move query to repository using FQL
    val result =
      retrievePost(id) flatMap {
        case Some(post) => update(post).map(Some(_))
        case None       => Future.successful(None)
      }

    result
  }

  def deletePost(id: String): Future[Option[Post]] =
    repository.remove(id)

}
