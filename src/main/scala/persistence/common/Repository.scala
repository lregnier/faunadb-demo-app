package persistence.common

import model.common.{Entity, Id}

import scala.concurrent.Future

/**
  * Base trait for implementing Repositories.
  *
  * It defines a set of base methods following DDD's
  * Repository pattern.
  */
trait Repository[A <: Entity] extends {
  def save(entity: A): Future[A]
  def saveAll(entities: A*): Future[Seq[A]]
  def remove(id: Id): Future[Option[A]]
  def find(id: Id): Future[Option[A]]
  def findAll(): Future[Seq[A]]
}
