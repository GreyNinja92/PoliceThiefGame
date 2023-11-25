import PoliceActor.{Command, Strategy}
import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

final case class Thief(id: Int, node_id: Int)

object ThiefActor {
  sealed trait Command
  final case class InitializeThief(replyTo: ActorRef[DefResponse]) extends Command
  final case class PossibleNextMoves(replyTo: ActorRef[NextMoves]) extends Command
  final case class MoveToNode(node_id: Int, replyTo: ActorRef[DefResponse]) extends Command
  final case class FindValuableNode(replyTo: ActorRef[DefResponse]) extends Command
  final case class FindPolice(replyTo: ActorRef[DefResponse]) extends Command
  final case class Strategy(str: String, replyTo: ActorRef[DefResponse]) extends Command
  final case class Result(replyTo: ActorRef[DefResponse]) extends Command


  def apply(): Behavior[Command] = registry(Set.empty)

  private def registry(thiefActors: Set[Thief]): Behavior[Command] =
    Behaviors.receiveMessage {
      case InitializeThief(replyTo) =>
        replyTo ! DefResponse(GraphOps.initializeThief())
        Behaviors.same
      case PossibleNextMoves(replyTo) =>
        replyTo ! NextMoves(GraphOps.possibleNextMoves(GraphOps.getThiefNode))
        Behaviors.same
      case MoveToNode(node_id, replyTo) =>
        replyTo ! DefResponse(GraphOps.moveThiefToNode(node_id))
        Behaviors.same
      case FindValuableNode(replyTo) =>
        replyTo ! DefResponse(GraphOps.findNearestNodeWithValue(GraphOps.getThiefNode))
        Behaviors.same
      case FindPolice(replyTo) =>
        replyTo ! DefResponse(s"Police is at node ${GraphOps.getPoliceNode.id}")
        Behaviors.same
      case Strategy(str, replyTo) =>
        if (str == "safe" || str == "random") {
          replyTo ! DefResponse(s"Playing with strategy: ${str}")
          GraphOps.initializeStrategy(isThief = true, str)
        } else {
          replyTo ! DefResponse("Enter a valid strategy")
        }
        Behaviors.same
      case Result(replyTo) =>
        if (GraphOps.thiefStrategy.nonEmpty) {
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