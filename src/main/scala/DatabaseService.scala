import java.sql.Timestamp
import scala.concurrent.{ExecutionContext, Future}

import com.typesafe.config.Config
import org.joda.time.DateTime
import slick.driver.SQLiteDriver.api._
import slick.jdbc.meta.MTable
import slick.profile.RelationalProfile

import Tables._

object DatabaseService {
  private def now = new Timestamp(DateTime.now.getMillis)

  /**
    * Creates table if it does not exist
    */
  private def ensureTable[T <: RelationalProfile#Table[_]](table: TableQuery[T])(implicit db: Database, ec: ExecutionContext) =
    MTable
      .getTables(table.shaped.value.tableName)
      .filter(_.nonEmpty)
      .map(_ ⇒ table.schema.create)
      .map(db.run)
}

class DatabaseService(val config: Config)(implicit val ec: ExecutionContext) {

  import DatabaseService._

  implicit val db = Database.forURL(config.getString("database.url"), driver = config.getString("database.driver"))

  ensureTable(posts)
  ensureTable(comments)

  def addPost(title: String, text: String): Future[Int] = db.run(
    posts += Post(None, title, text, now)
  )

  def getPostById(postId: Int): Future[Post] = db.run(
    posts.filter(_.id === postId).result.head
  )

  def getComments(postId: Int, offset: Int, limit: Int = 10): Future[Seq[Comment]] = db.run(
    comments.filter(_.postId === postId).sortBy(_.timeCreated.desc).drop(offset).take(limit).result
  )

  def getPostsWithCommentsCount(offset: Int, limit: Int = 10): Future[Seq[(Post, Int)]] = db.run(
    // SELECT p.*, COUNT(c.id) from posts p LEFT JOIN comments c ON c.postId = p.id GROUP BY p.id;
    (for {
      (p, c) ← posts joinLeft comments on (_.id === _.postId)
    } yield (p, c.map(_.id))).groupBy {
      case (p, c) ⇒ p
    }
      .map {
        case (p, group) ⇒ (p, group.map(_._2).countDistinct)
      }
      .drop(offset)
      .take(limit)
      .result
  )

  def addComment(postId: Int, text: String, author: Option[String]): Future[Int] = db.run {
    comments += Comment(None, postId, author.getOrElse("Guest"), text, now)
  }

  def patchPost(postId: Int, newTitle: Option[String], newText: Option[String]): Future[Int] = (newTitle, newText) match {
    case (Some(title), Some(text)) ⇒ db.run(
      posts.filter(_.id === postId).map(post ⇒ (post.title, post.text, post.timeUpdated)).update((title, text, Some(now)))
    )
    case (Some(title), None) ⇒ db.run(
      posts.filter(_.id === postId).map(post ⇒ (post.title, post.timeUpdated)).update((title, Some(now)))
    )
    case (None, Some(text)) ⇒ db.run(
      posts.filter(_.id === postId).map(post ⇒ (post.text, post.timeUpdated)).update((text, Some(now)))
    )
    case _ ⇒ Future.successful(0)
  }

  def deletePost(postId: Int): Future[Int] = db.run(posts.filter(_.id === postId).delete)

  def deleteComment(postId: Int, commentId: Int): Future[Int] = db.run(comments.filter(_.postId === postId).filter(_.id === commentId).delete)
}
