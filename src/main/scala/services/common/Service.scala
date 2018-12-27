package services.common

import scala.concurrent.ExecutionContext

/**
  * Base trait for implementing Domain Services
  */
trait Service {
  // TODO: using Scala's global ExecutionContext for now.
  // Evaluate the use of a dedicated ExecutionContext for
  // the services layer (bulkheading).
  implicit protected val ec: ExecutionContext = ExecutionContext.global
}
