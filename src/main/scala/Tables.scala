import java.sql.Timestamp

import slick.driver.SQLiteDriver.api._


object Tables {
  case class Post(id: Option[Int], title: String, text: String, timeCreated: Timestamp, timeUpdated: Option[Timestamp] = None)

  class Posts(tag: Tag) extends Table[Post](tag, "posts") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def title = column[String]("title")
    def text = column[String]("text")
    def timeCreated = column[Timestamp]("timeCreated")
    def timeUpdated = column[Option[Timestamp]]("timeUpdated")

    def * = (id.?, title, text, timeCreated, timeUpdated) <> (Post.tupled, Post.unapply)
  }

  case class Comment(id: Option[Int], postId: Int, author: String = "Guest", text: String, timeCreated: Timestamp)

  class Comments(tag: Tag) extends Table[Comment](tag, "comments") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def postId = column[Int]("postId")
    def author = column[String]("author", O.Default("Guest"))
    def text = column[String]("text", O.Length(500))
    def timeCreated = column[Timestamp]("timeCreated")

    def post = foreignKey("POST_FK", postId, posts)(_.id)

    def * = (id.?, postId, author, text, timeCreated) <> (Comment.tupled, Comment.unapply)
  }

  val posts = TableQuery[Posts]
  val comments = TableQuery[Comments]
}
