package rest.common

import akka.actor.{ActorSystem, CoordinatedShutdown, ExtendedActorSystem, Extension, ExtensionId, ExtensionIdProvider}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import com.typesafe.config.Config

import scala.util.{Failure, Success}

/**
  * RestServer backed by Akka HTTP. Alla Akka references are encapsulated here.
  */
object RestServer {

  private object BindFailure extends CoordinatedShutdown.Reason

  def start(restEndpoints: Seq[RestEndpoint] = Seq.empty): Unit = {
    implicit val system = ActorSystem("rest-server")
    implicit val ec = system.dispatcher
    implicit val mat = ActorMaterializer()
    val log = system.log

    val settings = RestServerSettings(system)
    val (host, port) = (settings.host, settings.port)
    val routes = restEndpoints.map(_.routes).reduce(_ ~ _)

    val shutdown = CoordinatedShutdown(system)

    Http()
      .bindAndHandle(routes, host, port)
      .onComplete {
        case Failure(error) =>
          log.error(error, "Shutting down, because cannot bind to {}:{}!", host, port)
          shutdown.run(BindFailure)

        case Success(binding) =>
          log.info("Listening for HTTP connections on {}", binding.localAddress)
          shutdown.addTask(CoordinatedShutdown.PhaseServiceUnbind, "http-server.unbind") { () =>
            binding.unbind()
          }
      }
  }

}

// Settings
class RestServerSettings(config: Config) extends Extension {
  private val restServerConfig = config.getConfig("rest-server")
  val host = restServerConfig.getString("host")
  val port = restServerConfig.getInt("port")
}

object RestServerSettings extends ExtensionId[RestServerSettings] with ExtensionIdProvider {
  override def createExtension(system: ExtendedActorSystem): RestServerSettings =
    new RestServerSettings(system.settings.config)

  override def lookup(): ExtensionId[_ <: Extension] = RestServerSettings

}
