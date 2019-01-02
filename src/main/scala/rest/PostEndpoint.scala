package rest

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{Directives, Route}
import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import model.{CreateReplacePostData, UpdatePostData}
import org.json4s.ext.JavaTimeSerializers
import org.json4s.{DefaultFormats, native}
import services.PostService

/**
  * Base trait for implementing Rest API endpoints
  */
trait RestEndpoint extends Directives with Json4sSupport {
  implicit val serialization = native.Serialization
  implicit val formats = DefaultFormats ++ JavaTimeSerializers.all

  def routes: Route
}

class PostEndpoint(postService: PostService) extends RestEndpoint {

  def createPost: Route =
    (pathEndOrSingleSlash & post & entity(as[CreateReplacePostData])) { data =>
      onSuccess(postService.createPost(data)) { post =>
        complete(StatusCodes.Created, post)
      }
    }

  def createSeveralPosts: Route =
    (pathEndOrSingleSlash & post & entity(as[Seq[CreateReplacePostData]])) { data =>
      onSuccess(postService.createSeveralPosts(data)) { post =>
        complete(StatusCodes.Created, post)
      }
    }

  def retrievePost: Route =
    (path(Segment) & get) { postId =>
      onSuccess(postService.retrievePost(postId)) {
        case Some(message) => complete(StatusCodes.OK, message)
        case None          => complete(StatusCodes.NotFound)
      }
    }

  def retrievePosts: Route =
    (pathEndOrSingleSlash & get) {
      onSuccess(postService.retrievePosts) { posts =>
        complete(StatusCodes.OK, posts)
      }
    }

  def retrievePostsByTags: Route = ???

  def replacePost: Route =
    (path(Segment) & put & entity(as[CreateReplacePostData])) { (id, data) =>
      onSuccess(postService.replacePost(id, data)) {
        case Some(post) => complete(StatusCodes.OK, post)
        case None       => complete(StatusCodes.NotFound)
      }
    }

  def updatePost: Route =
    (path(Segment) & put & entity(as[UpdatePostData])) { (id, data) =>
      onSuccess(postService.updatePost(id, data)) {
        case Some(post) => complete(StatusCodes.OK, post)
        case None       => complete(StatusCodes.NotFound)
      }
    }

  def deletePost: Route =
    (path(Segment) & delete) { postId =>
      onSuccess(postService.deletePost(postId)) {
        case Some(_) => complete(StatusCodes.NoContent)
        case _       => complete(StatusCodes.NotFound)
      }
    }

  override def routes: Route =
    pathPrefix("posts") {
      createPost ~
      createSeveralPosts ~
      retrievePost ~
      retrievePosts ~
      retrievePostsByTags ~
      updatePost ~
      deletePost
    }

}
