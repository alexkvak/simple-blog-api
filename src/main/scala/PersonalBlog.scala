import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory


object PersonalBlog extends App with Service {
  override implicit val system = ActorSystem()
  override implicit val executor = system.dispatcher
  implicit val materializer = ActorMaterializer()

  override val config = ConfigFactory.load()
  override val logger = Logging(system, getClass)

  override val dbService = new DatabaseService(config)

  // Http Service
  Http().bindAndHandle(routes, config.getString("http.interface"), config.getInt("http.port"))
}
