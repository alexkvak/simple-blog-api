import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.model.StatusCodes.{BadRequest, InternalServerError}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.UserCredentials
import akka.http.scaladsl.server.{UnsupportedRequestContentTypeRejection, RejectionHandler, MalformedRequestContentRejection}
import akka.stream.Materializer
import akka.util.Timeout
import com.typesafe.config.Config


trait Service extends ProtocolImplicits {

  import Protocol._

  implicit val askTimeout = Timeout(30 seconds)

  implicit val system: ActorSystem

  implicit def executor: ExecutionContextExecutor

  implicit val materializer: Materializer

  def config: Config

  val logger: LoggingAdapter
  val dbService: DatabaseService

  implicit val myRejectionHandler = RejectionHandler.newBuilder()
    .handle {
      case UnsupportedRequestContentTypeRejection(supportedContentTypes) ⇒
        val errorMsg = s"Unsupported content-type. Supported are: ${supportedContentTypes.mkString(", ")}"
        logger.error(errorMsg)
        complete(HttpResponse(BadRequest, entity = errorMsg))
      case MalformedRequestContentRejection(errorMsg, _) ⇒
        logger.error(errorMsg)
        complete(Marshal(BadRequest → ErrorResponse(errorMsg)).to[HttpResponse])
      case x ⇒
        val errorMsg = x.toString
        logger.error(errorMsg)
        complete(Marshal(InternalServerError → ErrorResponse(errorMsg)).to[HttpResponse])
    }
    .result()

  private def adminLogin = config.getString("auth.login")

  private def adminSecret = config.getString("auth.secret")

  private def authenticator: Authenticator[Boolean] = {
    case p@UserCredentials.Provided(username) if username == adminLogin ⇒ Some(p.verifySecret(adminSecret))
    case UserCredentials.Missing ⇒ None
  }

  private def secured = authenticateBasic("This action needs authentication", authenticator)

  val routes = {
    logRequestResult("personal-blog") {
      // GET /
      path("") {
        get {
          parameters("offset".as[Int] ? 0) { offset ⇒
            complete {
              dbService.getPostsWithCommentsCount(offset).map {
                _.map {
                  case (p, count) ⇒
                    BriefPostInfo(p.text, p.timeCreated.toString, p.timeUpdated.toString, count)
                }
              }.map(BlogInfo(blogTitle(), blogAuthor(), _))

              //              Marshal(InternalServerError → ErrorResponse("text")).to[HttpResponse]
            }
          }
        }
      } ~
        pathPrefix("post") {
          pathEnd {
            secured { authSuccess ⇒
              // PUT /post
              (put & entity(as[NewPost])) { newPost ⇒
                complete {
                  for {
                    _ ← dbService.addPost(newPost.title, newPost.text)
                  } yield DummySuccess()
                }
              }
            }
          } ~
            pathPrefix(IntNumber) { postId ⇒
              pathEnd {
                // GET /post/{id}
                get {
                  parameters("offset".as[Int] ? 0) { offset ⇒
                    complete {
                      for {
                        post ← dbService.getPostById(postId)
                        comments ← dbService.getComments(postId, offset)
                      }
                        yield GetPostResponse(blogTitle(), blogAuthor(), post.title, comments)
                    }
                  }
                } ~
                  secured { authSuccess ⇒
                    // PATCH /post/{id}
                    (patch & entity(as[PatchPost])) { patchPost ⇒
                      complete {
                        for {
                          updated ← dbService.patchPost(postId, patchPost.title, patchPost.text)
                        } yield DummySuccess(s"$updated row(s) updated")
                      }
                    } ~
                      // DELETE /post/{id}
                      delete {
                        complete {
                          for {
                            deleted ← dbService.deletePost(postId)
                          } yield DummySuccess(s"$deleted row(s) deleted")
                        }
                      }
                  }
              } ~
                pathPrefix("comment") {
                  // PUT /post/{id}/comment
                  (put & entity(as[NewComment])) { newComment ⇒
                    complete {
                      for {
                        _ ← dbService.addComment(postId, newComment.text, newComment.author)
                      } yield DummySuccess()
                    }
                  } ~
                    path(IntNumber) { commentId ⇒
                      // DELETE /post/{id}/comment/{id}
                      secured { authSuccess ⇒
                        delete {
                          complete {
                            for {
                              deleted ← dbService.deleteComment(postId, commentId)
                            } yield DummySuccess(s"$deleted row(s) deleted")
                          }
                        }
                      }
                    }
                }
            }
        }
    }
  }

  private def blogTitle() = config.getString("blog.title")

  private def blogAuthor() = config.getString("blog.author")

}