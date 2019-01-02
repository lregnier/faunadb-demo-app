package model

case class Post(id: String, title: String, tags: Seq[String])

case class CreateReplacePostData(title: String, tags: Seq[String])

case class UpdatePostData(title: Option[String], tags: Option[Seq[String]])
