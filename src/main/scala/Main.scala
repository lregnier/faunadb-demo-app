import com.typesafe.config.ConfigFactory
import faunadb.FaunaClient
import persistence.FaunaPostRepository
import persistence.common.FaunaSettings
import rest.PostEndpoint
import rest.common.RestServer
import services.PostService

trait PersistenceModule {
  lazy val faunaSettings = new FaunaSettings(ConfigFactory.load())
  lazy val faunaClient = FaunaClient(faunaSettings.apiKey)

  lazy val postRepository = new FaunaPostRepository(faunaClient)
}

trait DomainModule { self: PersistenceModule =>
  lazy val postService = new PostService(postRepository)
}

trait RestModule { self: DomainModule =>
  lazy val endpoints = Seq(
    PostEndpoint(postService)
  )

  RestServer.start(endpoints)
}

object Main extends App with PersistenceModule with DomainModule with RestModule
