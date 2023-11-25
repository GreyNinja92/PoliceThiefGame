import NetGraphAlgebraDefs.{NetGraph, NodeObject}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Random
import scala.jdk.CollectionConverters._

object GraphOps {
  val originalGraph: NetGraph = NetGraph.load(dir="", fileName = "original.json").get
  val perturbedGraph: NetGraph = NetGraph.load(dir="", fileName = "perturbed.json").get

  val policeNodes: ArrayBuffer[NodeObject] = ArrayBuffer[NodeObject]()
  val thiefNodes: ArrayBuffer[NodeObject] = ArrayBuffer[NodeObject]()

  val thiefStrategy: ArrayBuffer[String] = ArrayBuffer[String]()
  val policeStrategy: ArrayBuffer[String] = ArrayBuffer[String]()

  val result: ArrayBuffer[String] = ArrayBuffer[String]()

  val commonNodes: Array[NodeObject] = originalGraph.sm.nodes().asScala.intersect(perturbedGraph.sm.nodes().asScala).toArray

  def gameOver(): Unit = {
    policeNodes.clear()
    thiefNodes.clear()
    thiefStrategy.clear()
    policeStrategy.clear()
    result.clear()
  }

  def initializeStrategy(isThief: Boolean, str: String): Future[Unit] = Future {
    if (isThief) {
      thiefStrategy.addOne(str)
    } else {
      policeStrategy.addOne(str)
    }
    playWithStrategy()
  }

  def playWithStrategy(): Unit = {
    if(thiefNodes.length <= policeNodes.length && thiefStrategy.nonEmpty) {
      if(thiefStrategy.last == "safe") Strategies.SafeStrategy(true)
      if(thiefStrategy.last == "random") Strategies.RandomStrategy(true)
    }
    if(policeNodes.length <= thiefNodes.length && policeStrategy.nonEmpty){
      if (policeStrategy.last == "safe") Strategies.SafeStrategy(false)
      if (policeStrategy.last == "random") Strategies.RandomStrategy(false)
    }

    if(result.isEmpty && thiefStrategy.nonEmpty && policeStrategy.nonEmpty) {
      playWithStrategy()
    }
  }

  def getConfidenceScore(node: NodeObject): Float = {
    if(!originalGraph.sm.nodes().contains(node)) return 0
    val noOfNeighbours = perturbedGraph.sm.adjacentNodes(node).asScala.size
    val noOfNewNeighbours = perturbedGraph.sm.adjacentNodes(node).asScala.diff(originalGraph.sm.adjacentNodes(node).asScala).size
    val nodeIsIdentical = if (OutputParser.isNodeModified(node)) 0 else 1

    ((noOfNeighbours - noOfNewNeighbours) + nodeIsIdentical).toFloat / (noOfNeighbours + 1).toFloat
  }

  def canMoveBePerformedInOriginalGraph(node: NodeObject, next_id: Int): Boolean = {
    val nodeInOG = originalGraph.sm.nodes().asScala.filter({n => n.id == node.id}).head
    originalGraph.sm.adjacentNodes(nodeInOG).asScala.exists(_.id == next_id)
  }

  def possibleNextMoves(node: NodeObject): Map[String, String] = {
    if (node == null) return Map[String, String]("-1" -> "-1")
    perturbedGraph.sm.adjacentNodes(node).asScala.toList.map(node => {
      node.id.toString -> getConfidenceScore(node).toString
    }).toMap
  }

  def arePoliceAndThiefOnTheSameNode(): Boolean = {
    (thiefNodes.nonEmpty && policeNodes.nonEmpty && policeNodes.last.id == thiefNodes.last.id)
  }

  def thiefFoundValuableData(): Boolean = {
    (thiefNodes.nonEmpty && thiefNodes.last.valuableData)
  }

  def noMovesAvailable(node: NodeObject): Boolean = {
    possibleNextMoves(node).isEmpty
  }

  def initializePolice(): String = {
    println("Initializing police")
    policeNodes.addOne(
      commonNodes (
        Random.nextInt(commonNodes.length)
    ))

    if (this.arePoliceAndThiefOnTheSameNode()) {
      gameOver()
      return "Police found thief. Police wins"
    }

    if (this.noMovesAvailable(policeNodes.last)) {
      gameOver()
      return "No moves available for police. Police loses."
    }

    "Initialized Police"
  }

  def initializeThief(): String = {
    println("Initializing Thief")
    thiefNodes.addOne(
      commonNodes (
          Random.nextInt(commonNodes.length)
      )
    )

    if (this.arePoliceAndThiefOnTheSameNode()) {
      gameOver()
      return "Police found thief. Police wins"
    }

    if (this.thiefFoundValuableData()) {
      gameOver()
      return "Thief found valuable data. Thief wins"
    }

    if (this.noMovesAvailable(thiefNodes.last)) {
      gameOver()
      return "No moves available for thief. Thief loses."
    }

    "Initialized Thief"
  }

  def movePoliceToNode(node_id: Int): String = {
    if(policeNodes.size > thiefNodes.size) return "Thief's turn"

    if(GraphOps.possibleNextMoves(GraphOps.getPoliceNode).keys.toList.contains(node_id.toString)) {
      if(!GraphOps.canMoveBePerformedInOriginalGraph(GraphOps.getPoliceNode, node_id)) {
        gameOver()
        return "Move cannot be performed. Police loses"
      }

      policeNodes.addOne(perturbedGraph.sm.nodes().asScala.filter(node => node_id == node.id).head)

      if (this.arePoliceAndThiefOnTheSameNode()) {
        gameOver()
        return "Police found thief. Police wins"
      }

      if (this.noMovesAvailable(policeNodes.last)) {
        gameOver()
        return "No moves available for police. Police loses."
      }

      playWithStrategy()

      "Moved police to position"
    } else {
      "Failed to move to position"
    }
  }

  def moveThiefToNode(node_id: Int): String = {
    if(policeNodes.size < thiefNodes.size) return "Police's turn"

    if(GraphOps.possibleNextMoves(GraphOps.getThiefNode).keys.toList.contains(node_id.toString)) {
      if(!GraphOps.canMoveBePerformedInOriginalGraph(GraphOps.getThiefNode, node_id)) {
        gameOver()
        return "Move cannot be performed. Thief loses"
      }

      thiefNodes.addOne(perturbedGraph.sm.nodes().asScala.filter(node => node_id == node.id).head)

      if (this.arePoliceAndThiefOnTheSameNode()) {
        gameOver()
        return "Police found thief. Police wins"
      }

      if (this.thiefFoundValuableData()) {
        gameOver()
        return "Thief found valuable data. Thief wins"
      }

      if (this.noMovesAvailable(thiefNodes.last)) {
        gameOver()
        return "No moves available for thief. Thief loses."
      }

      playWithStrategy()

      "Moved thief to position"
    } else {
      "Failed to move to position"
    }
  }

  def findNearestNodeWithValue(node: NodeObject): String = {
    val queue = mutable.Queue[(NodeObject, Int)]()
    val visited = mutable.Set[NodeObject]()

    queue.enqueue((node, 0))

    while (queue.nonEmpty) {
      val (currentNode, distance) = queue.dequeue()
      visited.add(currentNode)

      if (currentNode.valuableData) {
        return s"Distance from valuable node : ${distance}"
      }

      val successors = perturbedGraph.sm.adjacentNodes(currentNode).asScala.toList
      successors.foreach(successor => {
        if(!visited.contains(successor)) {
          queue.enqueue((successor, distance + 1))
          visited.add(successor)
        }
      })
    }
    s"Distance from valuable node : -1"
  }

  def getPoliceNode: NodeObject = {
    if(policeNodes.isEmpty) return null
    policeNodes.last
  }

  def getThiefNode: NodeObject = {
    if(thiefNodes.isEmpty) return null
    thiefNodes.last
  }

}
