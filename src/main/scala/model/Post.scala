package model

import model.common.{DomainObject, Entity, Id}

case class Post(id: Id, title: String, content: String, tags: Seq[String]) extends Entity

case class CreateUpdatePostData(title: String, content: String, tags: Seq[String]) extends DomainObject
