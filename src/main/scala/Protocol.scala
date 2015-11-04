import java.sql.Timestamp

import spray.json._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import Tables._


object Protocol {
  // Response case classes
  case class BriefPostInfo(text: String, timeCreated: String, timeUpdated: String, commentsCount: Int)
  case class BlogInfo(title: String, author: String, posts: Seq[BriefPostInfo])
  case class DummySuccess(message: String = "OK")
  case class GetPostResponse(blogTitle: String, blogAuthor: String, postTitle: String, comments: Seq[Comment])

  // Incoming request case classes
  case class NewPost(title: String, text: String)
  case class PatchPost(title: Option[String], text: Option[String])
  case class NewComment(author: Option[String], text: String)

  case class ErrorResponse(message: String)
}

trait ProtocolImplicits extends DefaultJsonProtocol with SprayJsonSupport {
  import Protocol._

  implicit object TimestampJsonFormat extends JsonFormat[Timestamp] {
    def write(x: Timestamp) = JsNumber(x.getTime)
    def read(value: JsValue) = value match {
      case JsNumber(x) ⇒ new Timestamp(x.longValue)
      case x ⇒ deserializationError("Expected Timestamp as JsNumber, but got " + x)
    }
  }

  implicit val jsonBriefPostInfo = jsonFormat4(BriefPostInfo.apply)
  implicit val jsonBlogInfo = jsonFormat3(BlogInfo.apply)
  implicit val jsonDummySuccess = jsonFormat1(DummySuccess.apply)
  implicit val jsonComment = jsonFormat5(Comment.apply)
  implicit val jsonGetPostResponse = jsonFormat4(GetPostResponse.apply)

  implicit val jsonNewPost = jsonFormat2(NewPost.apply)
  implicit val jsonPatchPost = jsonFormat2(PatchPost.apply)
  implicit val jsonNewComment = jsonFormat2(NewComment.apply)

  implicit val jsonErrorResponse = jsonFormat1(ErrorResponse.apply)
}
