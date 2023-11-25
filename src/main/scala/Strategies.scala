import scala.jdk.CollectionConverters._

object Strategies {

  // Safe strategy picks nodes with the highest confidence values
  // To prevent getting stuck in infinite loops, we prioritize unseen nodes
  def SafeStrategy(isThief: Boolean): Unit = {
    val node = if(isThief) GraphOps.thiefNodes.last else GraphOps.policeNodes.last
    val allNodes = if(isThief) GraphOps.thiefNodes.map(node => node.id) else GraphOps.policeNodes.map(node => node.id)
    val player = if(isThief) NGSConstants.THIEF else NGSConstants.POLICE

    // Find nodes that are unseen
    val validMovesWithConfidence = GraphOps.possibleNextMoves(node).view.filterKeys(key => !allNodes.contains(key.toInt))
    // If no unseen node is left nearby, then pick between any seen node
    val moves = if(validMovesWithConfidence.isEmpty) GraphOps.possibleNextMoves(node).view else validMovesWithConfidence

    // Find the node with most confidence
    val safestMove = moves.maxBy {
      case (_, value) => value.toFloat
    }

    val newNode = GraphOps.perturbedGraph.sm.nodes().asScala.filter(n => n.id == safestMove._1.toInt).head

    // Check if the move is illegal
    if (!GraphOps.canMoveBePerformedInOriginalGraph(node, newNode.id)) {
      GraphOps.result.addOne(NGSConstants.MOVE_CANNOT_BE_PERFORMED(player))
      return
    }

    if(isThief) GraphOps.thiefNodes.addOne(newNode) else GraphOps.policeNodes.addOne(newNode)

    // Check if thief and police are on the same node
    if (GraphOps.arePoliceAndThiefOnTheSameNode()) {
      GraphOps.result.addOne(NGSConstants.POLICE_FOUND_THIEF)
      return
    }

    // Check if thief found valuable data
    if (GraphOps.thiefFoundValuableData()) {
      GraphOps.result.addOne(NGSConstants.THIEF_FOUND_DATA)
      return
    }

    // Check if the player arrived at a disjoint node
    if(GraphOps.noMovesAvailable(newNode)) {
      GraphOps.result.addOne(NGSConstants.NO_MOVES_AVAILABLE(player))
      return
    }
  }

  // Random strategy picks next node at random
  // We're still prioritizing unseen nodes to make the player traverse the entire graph
  def RandomStrategy(isThief: Boolean): Unit = {
    val node = if (isThief) GraphOps.thiefNodes.last else GraphOps.policeNodes.last
    val allNodes = if (isThief) GraphOps.thiefNodes.map(node => node.id) else GraphOps.policeNodes.map(node => node.id)
    val player = if (isThief) NGSConstants.THIEF else NGSConstants.POLICE

    // Find nodes that are unseen
    val validMovesWithConfidence = GraphOps.possibleNextMoves(node).view.filterKeys(key => !allNodes.contains(key.toInt))
    // If no unseen node is left nearby, then pick between any seen node
    val moves = if(validMovesWithConfidence.isEmpty) GraphOps.possibleNextMoves(node).view else validMovesWithConfidence

    val randomMove = moves.keys.toList(util.Random.nextInt(validMovesWithConfidence.keys.size))
    val newNode = GraphOps.perturbedGraph.sm.nodes().asScala.filter(n => n.id == randomMove.toInt).head

    // Check if the move is illegal
    if (!GraphOps.canMoveBePerformedInOriginalGraph(node, newNode.id)) {
      GraphOps.result.addOne(NGSConstants.MOVE_CANNOT_BE_PERFORMED(player))
      return
    }

    if (isThief) GraphOps.thiefNodes.addOne(newNode) else GraphOps.policeNodes.addOne(newNode)

    // Check if thief and police are on the same node
    if (GraphOps.arePoliceAndThiefOnTheSameNode()) {
      GraphOps.result.addOne(NGSConstants.POLICE_FOUND_THIEF)
      return
    }

    // Check if thief found valuable data
    if (GraphOps.thiefFoundValuableData()) {
      GraphOps.result.addOne(NGSConstants.THIEF_FOUND_DATA)
      return
    }

    // Check if the player arrived at a disjoint node
    if (GraphOps.noMovesAvailable(newNode)) {
      GraphOps.result.addOne(NGSConstants.NO_MOVES_AVAILABLE(player))
      return
    }
  }
}
