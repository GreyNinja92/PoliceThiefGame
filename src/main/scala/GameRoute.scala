import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route

import scala.concurrent.Future
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout
import PoliceActor._
import ThiefActor._

class GameRoute(policeRegistry: ActorRef[PoliceActor.Command], thiefRegistry: ActorRef[ThiefActor.Command])
               (implicit val system: ActorSystem[_]) {

  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import JsonFormats._

  private implicit val timeout: Timeout = Timeout.create(system.settings.config.getDuration("my-app.routes.ask-timeout"))

  def gameOver(): DefResponse = {
    GraphOps.gameOver()
    DefResponse("Reset game")
  }

  def initPolice(): Future[DefResponse] =
    policeRegistry.ask(InitializePolice.apply)
  def possibleNextMovesPolice(): Future[NextMoves] =
    policeRegistry.ask(PoliceActor.PossibleNextMoves.apply)
  def movePoliceToNode(node_id: Int): Future[DefResponse] =
    policeRegistry.ask(PoliceActor.MoveToNode(node_id, _))
  def findValuableNodePolice(): Future[DefResponse] =
    policeRegistry.ask(PoliceActor.FindValuableNode.apply)
  def findThief(): Future[DefResponse] =
    policeRegistry.ask(PoliceActor.FindThief.apply)
  def playStrategyPolice(str: String): Future[DefResponse] =
    policeRegistry.ask(PoliceActor.Strategy(str, _))
  def showResultPolice(): Future[DefResponse] =
    policeRegistry.ask(PoliceActor.Result.apply)

  def initThief(): Future[DefResponse] =
    thiefRegistry.ask(InitializeThief.apply)
  def possibleNextMovesThief(): Future[NextMoves] =
    thiefRegistry.ask(ThiefActor.PossibleNextMoves.apply)
  def moveThiefToNode(node_id: Int): Future[DefResponse] =
    thiefRegistry.ask(ThiefActor.MoveToNode(node_id, _))
  def findValuableNodeThief(): Future[DefResponse] =
    thiefRegistry.ask(ThiefActor.FindValuableNode.apply)
  def findPolice(): Future[DefResponse] =
    thiefRegistry.ask(ThiefActor.FindPolice.apply)
  def playStrategyThief(str: String): Future[DefResponse] =
    thiefRegistry.ask(ThiefActor.Strategy(str, _))
  def showResultThief(): Future[DefResponse] =
    thiefRegistry.ask(ThiefActor.Result.apply)

  val policeRoutes: Route =
    pathPrefix("police") {
      concat(
        pathEnd {
          concat(
            get {
              complete(initPolice())
            }
          )
        },
        path("possibleMoves") {
          get {
            complete(possibleNextMovesPolice())
          }
        },
        path("findValuableNode") {
          get {
            complete(findValuableNodePolice())
          }
        },
        path("findThief") {
          get {
            complete(findThief())
          }
        },
        path("result"){
          get {
            complete(showResultPolice())
          }
        },
        pathPrefix("move") {
          path(IntNumber) { node_id =>
            get {
              complete(movePoliceToNode(node_id))
            }
          }
        },
        pathPrefix("strategy") {
          path(Segment) { str =>
            get {
              complete(playStrategyPolice(str))
            }
          }
        }
      )
    }

  val thiefRoutes: Route =
    pathPrefix("thief") {
      concat(
        pathEnd {
          concat(
            get {
              complete(initThief())
            }
          )
        },
        path("possibleMoves") {
          get {
            complete(possibleNextMovesThief())
          }
        },
        path("findValuableNode") {
          get {
            complete(findValuableNodeThief())
          }
        },
        path("findPolice") {
          get {
            complete(findPolice())
          }
        },
        path("result") {
          get {
            complete(showResultThief())
          }
        },
        pathPrefix("move") {
          path(IntNumber) { node_id =>
            get {
              complete(moveThiefToNode(node_id))
            }
          }
        },
        pathPrefix("strategy") {
          path(Segment) { str =>
            get {
              complete(playStrategyThief(str))
            }
          }
        }
      )
    }

  val routes: Route = concat(policeRoutes, thiefRoutes,
    path("restart") {
    get {
      complete(gameOver())
    }
  })

}
