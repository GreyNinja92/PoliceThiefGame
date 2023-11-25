import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import org.slf4j.{Logger, LoggerFactory}

import scala.util.Failure
import scala.util.Success

object Main {
  val logger: Logger = LoggerFactory.getLogger(Main.getClass)

  // Initialize HTTP Server
  private def startHttpServer(routes: Route)(implicit system: ActorSystem[_]): Unit = {
    // Akka HTTP still needs a classic ActorSystem to start
    import system.executionContext

//    AWS EC2
//    val futureBinding = Http().newServerAt(NGSConstants.AWS_URL, 8080).bind(routes)
    val futureBinding = Http().newServerAt(NGSConstants.URL, 8080).bind(routes)
    futureBinding.onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        logger.info(s"Server online at http://${address.getHostString}:${address.getPort}/")
      case Failure(ex) =>
        logger.info("Failed to bind HTTP endpoint, terminating system", ex)
        system.terminate()
    }
  }

  def main(args: Array[String]): Unit = {
    // Parse difference.yaml
    OutputParser.parseGoldenYAML(NGSConstants.DIFFERENCE_YAML)

    // Start HTTP Server
    val rootBehavior = Behaviors.setup[Nothing] { context =>
      val policeActor = context.spawn(PoliceActor(), NGSConstants.POLICE_ACTOR)
      val thiefActor = context.spawn(ThiefActor(), NGSConstants.THIEF_ACTOR)

      context.watch(policeActor)
      context.watch(thiefActor)

      val routes = new GameRoute(policeActor, thiefActor)(context.system)
      startHttpServer(routes.routes)(context.system)

      Behaviors.empty
    }
    val system = ActorSystem[Nothing](rootBehavior, NGSConstants.SERVER)

  }
}