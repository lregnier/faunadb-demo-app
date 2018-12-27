package model.common

/**
  * Marker trait for Domain Objects
  */
trait DomainObject

/**
  * Default Identity implementation. It defines a
  * String Identity with UUID format.
  *
  * @param value must be a valid UUID string
  */
case class Id(value: String) extends DomainObject

/**
  * Companion object for Id
  */
object Id {
  def unassigned(): Id = Id("unassigned-id")
}

/**
  * Base trait for building Entities
  */
trait Entity extends DomainObject {
  val id: Id
}
