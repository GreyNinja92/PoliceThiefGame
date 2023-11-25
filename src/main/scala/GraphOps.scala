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

  // Loading the original & perturbed graphs
  val originalGraph: NetGraph = NetGraph.loadGraph(dir="", fileName = NGSConstants.ORIGINAL_GRAPH).get
  val perturbedGraph: NetGraph = NetGraph.loadGraph(dir="", fileName = NGSConstants.PERTURBED_GRAPH).get

  // These arrays are used to store the current & past locations of police & thief
  val policeNodes: ArrayBuffer[NodeObject] = ArrayBuffer[NodeObject]()
  val thiefNodes: ArrayBuffer[NodeObject] = ArrayBuffer[NodeObject]()

  // These arrays are used to store the current strategies (if any) being used for thief and/or police
  val thiefStrategy: ArrayBuffer[String] = ArrayBuffer[String]()
  val policeStrategy: ArrayBuffer[String] = ArrayBuffer[String]()

  // This array stores the result after executing a strategy
  val result: ArrayBuffer[String] = ArrayBuffer[String]()

  // This array stores the nodes present in both graphs i.e. original & perturbed
  val commonNodes: Array[NodeObject] = originalGraph.sm.nodes().asScala.intersect(perturbedGraph.sm.nodes().asScala).toArray

  // This function clears all variables
  def gameOver(): Unit = {
    policeNodes.clear()
    thiefNodes.clear()
    thiefStrategy.clear()
    policeStrategy.clear()
    result.clear()
  }

  // This function initializes strategy for thief and/or police
  def initializeStrategy(isThief: Boolean, str: String): Future[Unit] = Future {
    if (isThief) {
      thiefStrategy.addOne(str)
    } else {
      policeStrategy.addOne(str)
    }
    playWithStrategy()
  }

  // This function moves the police & thief nodes based on the strategy
  // It takes into account turn by checking the length of thiefNodes & policeNodes i.e. after police moves, police has to wait for thief to move
  // If both clients are using strategies, then this function recursively calls itself to execute moves for each player based on strategy and turns
  // This function calls the respective functions in Strategies file to execute moves for thief and/or police based on the strategy chosen
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

  // This function returns the confidence score for a node. It calculates it based on the information derived from the yaml file.
  // For e.g. if node & 4 edges are identical between two graphs but 1 edge is not, then the confidence score is 5/6
  def getConfidenceScore(node: NodeObject): Float = {
    if(!originalGraph.sm.nodes().contains(node)) return 0
    val noOfNeighbours = perturbedGraph.sm.adjacentNodes(node).asScala.size
    val noOfNewNeighbours = perturbedGraph.sm.adjacentNodes(node).asScala.diff(originalGraph.sm.adjacentNodes(node).asScala).size
    val nodeIsIdentical = if (OutputParser.isNodeChanged(node)) 0 else 1

    ((noOfNeighbours - noOfNewNeighbours) + nodeIsIdentical).toFloat / (noOfNeighbours + 1).toFloat
  }

  // This function checks you can move from node to next_id in the original graph
  def canMoveBePerformedInOriginalGraph(node: NodeObject, next_id: Int): Boolean = {
    val nodeInOG = originalGraph.sm.nodes().asScala.filter({n => n.id == node.id}).head
    originalGraph.sm.adjacentNodes(nodeInOG).asScala.exists(_.id == next_id)
  }

  // This function computes the next moves possible from a node
  def possibleNextMoves(node: NodeObject): Map[String, String] = {
    if (node == null) return Map[String, String]("-1" -> "-1")
    perturbedGraph.sm.adjacentNodes(node).asScala.toList.map(node => {
      node.id.toString -> getConfidenceScore(node).toString
    }).toMap
  }

  // This function checks if Police & Thief are on the same node
  def arePoliceAndThiefOnTheSameNode(): Boolean = {
    (thiefNodes.nonEmpty && policeNodes.nonEmpty && policeNodes.last.id == thiefNodes.last.id)
  }

  // This function checks if thief found valuable data
  def thiefFoundValuableData(): Boolean = {
    (thiefNodes.nonEmpty && thiefNodes.last.valuableData)
  }

  // This function checks if moves are possible from a node
  def noMovesAvailable(node: NodeObject): Boolean = {
    possibleNextMoves(node).isEmpty
  }

  // This function initializes Police
  // It also checks if after initialization, police is on the same node as thief & if police is on a disjoint node
  // If so, then the game ends
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


  // This function initializes Thief
  // It also checks if after initialization, police is on the same node as thief, if thief is on a disjoint node &
  // if thief is on a node with valuable data
  // If so, then the game ends
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

  // This function moves police from the current node to node_id
  // It checks if the move is possible for e.g. are there outgoing edges between the two nodes in both graphs
  // It also checks if this move transfers police to thief's node or to a disjoint node
  // Lastly, it calls strategy function to execute thief's move if thief is using a strategy
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


  // This function moves thief from the current node to node_id
  // It checks if the move is possible for e.g. are there outgoing edges between the two nodes in both graphs
  // It also checks if this move transfers thief to police's node or  to a disjoint node or to a node with valuable data
  // Lastly, it calls strategy function to execute police's move if police is using a strategy
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

  // This function is used to find the distance of a valuable node from the node passed as a parameter
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

  // Simple getter for police's current location
  def getPoliceNode: NodeObject = {
    if(policeNodes.isEmpty) return null
    policeNodes.last
  }

  // Simple getter for thief's current location
  def getThiefNode: NodeObject = {
    if(thiefNodes.isEmpty) return null
    thiefNodes.last
  }

}
