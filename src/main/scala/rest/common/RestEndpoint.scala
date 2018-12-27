package rest.common

import akka.http.scaladsl.server.{Directives, Route}
import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import org.json4s.ext.JavaTimeSerializers
import org.json4s.{DefaultFormats, native}

/**
  * Base trait for implementing Rest API endpoints
  */
trait RestEndpoint extends Directives with Json4sJacksonSupport {
  def routes: Route
}

/**
  * Json4sSupport backed by native (Lift JSON) serialization.
  *
  * Json4sSupport allows automatic (un)marshalling from/into Json
  * without providing any custom type class in scope.
  *
  * More info at: https://github.com/hseeberger/akka-http-json
  */
trait Json4sJacksonSupport extends Json4sSupport {
  implicit val serialization = native.Serialization
  implicit val formats = DefaultFormats ++ JavaTimeSerializers.all
}
