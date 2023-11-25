import NetGraphAlgebraDefs.{NetGraph, NodeObject}
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Random
import scala.jdk.CollectionConverters._

object GraphOps {
  val logger: Logger = LoggerFactory.getLogger(GraphOps.getClass)

  val originalGraph: NetGraph = NetGraph.load(dir="", fileName = NGSConstants.ORIGINAL_GRAPH).get
  val perturbedGraph: NetGraph = NetGraph.load(dir="", fileName = NGSConstants.PERTURBED_GRAPH).get

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
      if(thiefStrategy.last == NGSConstants.SAFE) Strategies.SafeStrategy(true)
      if(thiefStrategy.last == NGSConstants.RANDOM) Strategies.RandomStrategy(true)
    }
    if(policeNodes.length <= thiefNodes.length && policeStrategy.nonEmpty){
      if (policeStrategy.last == NGSConstants.SAFE) Strategies.SafeStrategy(false)
      if (policeStrategy.last == NGSConstants.RANDOM) Strategies.RandomStrategy(false)
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
    logger.info("Initializing police")
    policeNodes.addOne(
      commonNodes (
        Random.nextInt(commonNodes.length)
    ))

    if (this.arePoliceAndThiefOnTheSameNode()) {
      gameOver()
      result.addOne(NGSConstants.POLICE_FOUND_THIEF)
      return NGSConstants.POLICE_FOUND_THIEF
    }

    if (this.noMovesAvailable(policeNodes.last)) {
      gameOver()
      result.addOne(NGSConstants.NO_MOVES_AVAILABLE(NGSConstants.POLICE))
      return NGSConstants.NO_MOVES_AVAILABLE(NGSConstants.POLICE)
    }

    NGSConstants.INITIALIZED_POLICE
  }

  def initializeThief(): String = {
    logger.info("Initializing Thief")
    thiefNodes.addOne(
      commonNodes (
          Random.nextInt(commonNodes.length)
      )
    )

    if (this.arePoliceAndThiefOnTheSameNode()) {
      gameOver()
      result.addOne(NGSConstants.POLICE_FOUND_THIEF)
      return NGSConstants.POLICE_FOUND_THIEF
    }

    if (this.thiefFoundValuableData()) {
      gameOver()
      result.addOne(NGSConstants.THIEF_FOUND_DATA)
      return NGSConstants.THIEF_FOUND_DATA
    }

    if (this.noMovesAvailable(thiefNodes.last)) {
      gameOver()
      result.addOne(NGSConstants.NO_MOVES_AVAILABLE(NGSConstants.THIEF))
      return NGSConstants.NO_MOVES_AVAILABLE(NGSConstants.THIEF)
    }

    NGSConstants.INITIALIZED_THIEF
  }

  def movePoliceToNode(node_id: Int): String = {
    if(policeNodes.size > thiefNodes.size) return NGSConstants.THIEF_TURN

    if(GraphOps.possibleNextMoves(GraphOps.getPoliceNode).keys.toList.contains(node_id.toString)) {
      if(!GraphOps.canMoveBePerformedInOriginalGraph(GraphOps.getPoliceNode, node_id)) {
        gameOver()
        result.addOne(NGSConstants.MOVE_CANNOT_BE_PERFORMED(NGSConstants.POLICE))
        return NGSConstants.MOVE_CANNOT_BE_PERFORMED(NGSConstants.POLICE)
      }

      policeNodes.addOne(perturbedGraph.sm.nodes().asScala.filter(node => node_id == node.id).head)

      if (this.arePoliceAndThiefOnTheSameNode()) {
        gameOver()
        result.addOne(NGSConstants.POLICE_FOUND_THIEF)
        return NGSConstants.POLICE_FOUND_THIEF
      }

      if (this.noMovesAvailable(policeNodes.last)) {
        gameOver()
        result.addOne(NGSConstants.NO_MOVES_AVAILABLE(NGSConstants.POLICE))
        return NGSConstants.NO_MOVES_AVAILABLE(NGSConstants.POLICE)
      }

      playWithStrategy()

      NGSConstants.MOVED_POLICE
    } else {
      NGSConstants.FAILED_TO_MOVE
    }
  }

  def moveThiefToNode(node_id: Int): String = {
    if(policeNodes.size < thiefNodes.size) return NGSConstants.POLICE_TURN

    if(GraphOps.possibleNextMoves(GraphOps.getThiefNode).keys.toList.contains(node_id.toString)) {
      if(!GraphOps.canMoveBePerformedInOriginalGraph(GraphOps.getThiefNode, node_id)) {
        gameOver()
        result.addOne(NGSConstants.MOVE_CANNOT_BE_PERFORMED(NGSConstants.THIEF))
        return NGSConstants.MOVE_CANNOT_BE_PERFORMED(NGSConstants.THIEF)
      }

      thiefNodes.addOne(perturbedGraph.sm.nodes().asScala.filter(node => node_id == node.id).head)

      if (this.arePoliceAndThiefOnTheSameNode()) {
        gameOver()
        result.addOne(NGSConstants.POLICE_FOUND_THIEF)
        return NGSConstants.POLICE_FOUND_THIEF
      }

      if (this.thiefFoundValuableData()) {
        gameOver()
        result.addOne(NGSConstants.THIEF_FOUND_DATA)
        return NGSConstants.THIEF_FOUND_DATA
      }

      if (this.noMovesAvailable(thiefNodes.last)) {
        gameOver()
        result.addOne(NGSConstants.NO_MOVES_AVAILABLE(NGSConstants.THIEF))
        return NGSConstants.NO_MOVES_AVAILABLE(NGSConstants.THIEF)
      }

      playWithStrategy()

      NGSConstants.MOVED_THIEF
    } else {
      NGSConstants.FAILED_TO_MOVE
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
        return NGSConstants.DISTANCE_FROM_NODE(distance)
      }

      val successors = perturbedGraph.sm.adjacentNodes(currentNode).asScala.toList
      successors.foreach(successor => {
        if(!visited.contains(successor)) {
          queue.enqueue((successor, distance + 1))
          visited.add(successor)
        }
      })
    }
    NGSConstants.DISTANCE_FROM_NODE(-1)
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
