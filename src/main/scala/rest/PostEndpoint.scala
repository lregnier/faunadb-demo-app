package rest

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import model.CreateUpdatePostData
import model.common.Id
import rest.common.RestEndpoint
import services.PostService

object PostEndpoint {
  def apply(mailboxService: PostService): PostEndpoint = new PostEndpoint(mailboxService)
}

class PostEndpoint(postService: PostService) extends RestEndpoint {

  def createPost: Route =
    (pathEndOrSingleSlash & post & entity(as[CreateUpdatePostData])) { data =>
      onSuccess(postService.createPost(data)) { post =>
        complete(StatusCodes.Created, post)
      }
    }

  def getPost: Route =
    (path(Segment) & get) { postId =>
      onSuccess(postService.retrievePost(Id(postId))) {
        case Some(message) => complete(StatusCodes.OK, message)
        case None          => complete(StatusCodes.NotFound)
      }
    }

  def updatePost: Route =
    (path(Segment) & put & entity(as[CreateUpdatePostData])) { (id, data) =>
      onSuccess(postService.updatePost(Id(id), data)) { post =>
        complete(StatusCodes.OK, post)
      }
    }

  def deletePost: Route =
    (path(Segment) & delete) { postId =>
      onSuccess(postService.deletePost(Id(postId))) {
        case Some(_) => complete(StatusCodes.NoContent)
        case _       => complete(StatusCodes.NotFound)
      }
    }

  override def routes: Route =
    pathPrefix("posts") {
      createPost ~
      getPost ~
      updatePost ~
      deletePost
    }

}
