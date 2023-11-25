import NetGraphAlgebraDefs.NodeObject
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class GraphOpsTest extends AnyFlatSpec with Matchers {
  behavior.of("Testing Graph Operations")

  val ogNode = GraphOps.originalGraph.sm.nodes().toArray.head.asInstanceOf[NodeObject]
  val perturbedNode = GraphOps.perturbedGraph.sm.nodes().toArray().filter(node => node.asInstanceOf[NodeObject].id == 21).head.asInstanceOf[NodeObject]

  it should "should load graphs" in {
    assert(GraphOps.originalGraph != null)
    assert(GraphOps.perturbedGraph != null)
  }

  it should "clear all nodes when game over" in {
    GraphOps.policeNodes.addOne(ogNode)
    GraphOps.thiefNodes.addOne(ogNode)
    GraphOps.thiefStrategy.addOne("temp")
    GraphOps.policeStrategy.addOne("temp")
    GraphOps.result.addOne("temp")

    GraphOps.gameOver()

    assert(GraphOps.policeNodes.isEmpty)
    assert(GraphOps.thiefNodes.isEmpty)
    assert(GraphOps.thiefStrategy.isEmpty)
    assert(GraphOps.policeStrategy.isEmpty)
    assert(GraphOps.result.isEmpty)
  }

  it should "output confidence score 1.0 for unmodified nodes" in {
    GraphOps.getConfidenceScore(ogNode) shouldBe 1.0f
  }

  it should "output confidence score 0.0 for new nodes" in {
    GraphOps.getConfidenceScore(perturbedNode) shouldBe 0.0f
  }

  it should "output possible next moves for nodes" in {
    assert(GraphOps.possibleNextMoves(perturbedNode).isInstanceOf[Map[String, String]])
    GraphOps.possibleNextMoves(perturbedNode).keys.size shouldBe 1
  }
}
