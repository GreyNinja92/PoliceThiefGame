import NetGraphAlgebraDefs.NodeObject
import ThiefActor.Command
import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

import scala.collection.immutable

final case class Police(id: Int, node_id: Int)
final case class NextMoves(moves: Map[String, String])
final case class DefResponse(response: String)

object PoliceActor {
  sealed trait Command
  final case class InitializePolice(replyTo: ActorRef[DefResponse]) extends Command
  final case class PossibleNextMoves(replyTo: ActorRef[NextMoves]) extends Command
  final case class MoveToNode(node_id: Int, replyTo: ActorRef[DefResponse]) extends Command
  final case class FindValuableNode(replyTo: ActorRef[DefResponse]) extends Command
  final case class FindThief(replyTo: ActorRef[DefResponse]) extends Command
  final case class Strategy(str: String, replyTo: ActorRef[DefResponse]) extends Command
  final case class Result(replyTo: ActorRef[DefResponse]) extends Command

  def apply(): Behavior[Command] = registry(Set.empty)

  private def registry(policeActors: Set[Police]): Behavior[Command] =
    Behaviors.receiveMessage {
      case InitializePolice(replyTo) =>
        replyTo ! DefResponse(GraphOps.initializePolice())
        Behaviors.same
      case PossibleNextMoves(replyTo) =>
        replyTo ! NextMoves(GraphOps.possibleNextMoves(GraphOps.getPoliceNode))
        Behaviors.same
      case MoveToNode(node_id, replyTo) =>
        replyTo ! DefResponse(GraphOps.movePoliceToNode(node_id))
        Behaviors.same
      case FindValuableNode(replyTo) =>
        replyTo ! DefResponse(GraphOps.findNearestNodeWithValue(GraphOps.getPoliceNode))
        Behaviors.same
      case FindThief(replyTo) =>
        replyTo ! DefResponse(s"Thief is at node ${GraphOps.getThiefNode.id}")
        Behaviors.same
      case Strategy(str, replyTo) =>
        if (str == "safe" || str == "random") {
          replyTo ! DefResponse(s"Playing with strategy: ${str}")
          GraphOps.initializeStrategy(isThief = false, str)
        } else {
          replyTo ! DefResponse("Enter a valid strategy")
        }
        Behaviors.same
      case Result(replyTo) =>
        if (GraphOps.policeStrategy.nonEmpty) {
          if (GraphOps.result.nonEmpty) {
            replyTo ! DefResponse(GraphOps.result.last)
          } else {
            replyTo ! DefResponse("Still playing game")
          }
        } else {
          replyTo ! DefResponse("Set a strategy first")
        }
        Behaviors.same
    }
}


