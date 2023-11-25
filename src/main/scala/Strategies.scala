import scala.jdk.CollectionConverters._
import scala.util.Random

object Strategies {

  def SafeStrategy(isThief: Boolean): Unit = {
    val node = if(isThief) GraphOps.thiefNodes.last else GraphOps.policeNodes.last
    val allNodes = if(isThief) GraphOps.thiefNodes.map(node => node.id) else GraphOps.policeNodes.map(node => node.id)
    val player = if(isThief) "Thief" else "Police"

    val validMovesWithConfidence = GraphOps.possibleNextMoves(node).view.filterKeys(key => !allNodes.contains(key.toInt))
    val moves = if(validMovesWithConfidence.isEmpty) GraphOps.possibleNextMoves(node).view else validMovesWithConfidence

    val safestMove = moves.maxBy {
      case (_, value) => value.toFloat
    }

    val newNode = GraphOps.perturbedGraph.sm.nodes().asScala.filter(n => n.id == safestMove._1.toInt).head

    if (!GraphOps.canMoveBePerformedInOriginalGraph(node, newNode.id)) {
      GraphOps.result.addOne(s"Move cannot be performed. ${player} loses")
      return
    }

    if(isThief) GraphOps.thiefNodes.addOne(newNode) else GraphOps.policeNodes.addOne(newNode)

    if (GraphOps.arePoliceAndThiefOnTheSameNode()) {
      GraphOps.result.addOne("Police found thief. Police wins")
      return
    }

    if (GraphOps.thiefFoundValuableData()) {
      GraphOps.result.addOne("Thief found valuable data. Thief wins")
      return
    }

    if(GraphOps.noMovesAvailable(newNode)) {
      GraphOps.result.addOne(s"No moves available. ${player} loses")
      return
    }
  }

  def RandomStrategy(isThief: Boolean): Unit = {
    val node = if (isThief) GraphOps.thiefNodes.last else GraphOps.policeNodes.last
    val allNodes = if (isThief) GraphOps.thiefNodes.map(node => node.id) else GraphOps.policeNodes.map(node => node.id)
    val player = if (isThief) "Thief" else "Police"

    val validMovesWithConfidence = GraphOps.possibleNextMoves(node).view.filterKeys(key => !allNodes.contains(key.toInt))
    val moves = if(validMovesWithConfidence.isEmpty) GraphOps.possibleNextMoves(node).view else validMovesWithConfidence

    val randomMove = moves.keys.toList(util.Random.nextInt(validMovesWithConfidence.keys.size))
    val newNode = GraphOps.perturbedGraph.sm.nodes().asScala.filter(n => n.id == randomMove.toInt).head

    if (!GraphOps.canMoveBePerformedInOriginalGraph(node, newNode.id)) {
      GraphOps.result.addOne(s"Move cannot be performed. ${player} loses")
      return
    }

    if (isThief) GraphOps.thiefNodes.addOne(newNode) else GraphOps.policeNodes.addOne(newNode)

    if (GraphOps.arePoliceAndThiefOnTheSameNode()) {
      GraphOps.result.addOne("Police found thief. Police wins")
      return
    }

    if (GraphOps.thiefFoundValuableData()) {
      GraphOps.result.addOne("Thief found valuable data. Thief wins")
      return
    }

    if (GraphOps.noMovesAvailable(newNode)) {
      GraphOps.result.addOne(s"No moves available. ${player} loses")
      return
    }
  }
}
