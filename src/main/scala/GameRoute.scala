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

// This class contains all routes for the api
class GameRoute(policeRegistry: ActorRef[PoliceActor.Command], thiefRegistry: ActorRef[ThiefActor.Command])
               (implicit val system: ActorSystem[_]) {

  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import JsonFormats._

  private implicit val timeout: Timeout = Timeout.create(system.settings.config.getDuration("app.routes.ask-timeout"))

  def gameOver(): DefResponse = {
    GraphOps.gameOver()
    DefResponse(NGSConstants.RESET_GAME)
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

  // These routes are explained in README
  val policeRoutes: Route =
    pathPrefix(NGSConstants.PATH_POLICE) {
      concat(
        pathEnd {
          concat(
            get {
              complete(initPolice())
            }
          )
        },
        path(NGSConstants.POSSIBLE_MOVES) {
          get {
            complete(possibleNextMovesPolice())
          }
        },
        path(NGSConstants.FIND_VALUABLE_NODE) {
          get {
            complete(findValuableNodePolice())
          }
        },
        path(NGSConstants.FIND_THIEF) {
          get {
            complete(findThief())
          }
        },
        path(NGSConstants.RESULT){
          get {
            complete(showResultPolice())
          }
        },
        pathPrefix(NGSConstants.MOVE) {
          path(IntNumber) { node_id =>
            get {
              complete(movePoliceToNode(node_id))
            }
          }
        },
        pathPrefix(NGSConstants.STRATEGY) {
          path(Segment) { str =>
            get {
              complete(playStrategyPolice(str))
            }
          }
        }
      )
    }

  val thiefRoutes: Route =
    pathPrefix(NGSConstants.PATH_THIEF) {
      concat(
        pathEnd {
          concat(
            get {
              complete(initThief())
            }
          )
        },
        path(NGSConstants.POSSIBLE_MOVES) {
          get {
            complete(possibleNextMovesThief())
          }
        },
        path(NGSConstants.FIND_VALUABLE_NODE) {
          get {
            complete(findValuableNodeThief())
          }
        },
        path(NGSConstants.FIND_POLICE) {
          get {
            complete(findPolice())
          }
        },
        path(NGSConstants.RESULT) {
          get {
            complete(showResultThief())
          }
        },
        pathPrefix(NGSConstants.MOVE) {
          path(IntNumber) { node_id =>
            get {
              complete(moveThiefToNode(node_id))
            }
          }
        },
        pathPrefix(NGSConstants.STRATEGY) {
          path(Segment) { str =>
            get {
              complete(playStrategyThief(str))
            }
          }
        }
      )
    }

  val routes: Route = concat(policeRoutes, thiefRoutes,
    path(NGSConstants.RESTART) {
    get {
      complete(gameOver())
    }
  })

}
