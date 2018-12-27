package services

import model.{CreateUpdatePostData, Post}
import model.common.Id
import persistence.PostRepository
import services.common.Service

import scala.concurrent.Future

class PostService(repository: PostRepository) extends Service {

  def createPost(createUpdateData: CreateUpdatePostData): Future[Post] = {
    val postEntity =
      Post(
        Id.unassigned(), // Id will be automatically assigned by the Repository once saved
        createUpdateData.title,
        createUpdateData.content,
        createUpdateData.tags
      )
    repository.save(postEntity)
  }

  def createAllPost(createUpdateData: Seq[Post]): Future[Seq[Post]] = {
    val postEntities =
      createUpdateData.map { data =>
        Post(Id.unassigned(), data.title, data.content, data.tags)
      }

    repository.saveAll(postEntities: _*)
  }

  def retrievePost(id: Id): Future[Option[Post]] =
    repository.find(id)

  def updatePost(id: Id, data: CreateUpdatePostData): Future[Option[Post]] = {
    def update(post: Post): Future[Post] =
      repository.save(post.copy(title = data.title, content = data.content, tags = data.tags))

    val result =
      retrievePost(id) flatMap {
        case Some(post) => update(post).map(Some(_))
        case None       => Future.successful(None)
      }

    result
  }

  def deletePost(id: Id): Future[Option[Post]] =
    repository.remove(id)

  def listPosts(): Future[Seq[Post]] =
    repository.findAll()

  def listByTags(tags: Seq[String]): Future[Seq[Post]] = ???

}
