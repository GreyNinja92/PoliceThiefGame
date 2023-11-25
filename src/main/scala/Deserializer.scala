import NetGraphAlgebraDefs.{Action, NetGraph, NetModelAlgebra, NodeObject}
import io.circe.Decoder._
import org.slf4j.{Logger, LoggerFactory}
import io.circe._
import io.circe.parser._
import org.apache.hadoop.fs.{FileSystem, Path}
import org.apache.hadoop.conf.Configuration

import scala.io.Source

object Deserializer {
  val logger: Logger = LoggerFactory.getLogger(Deserializer.getClass)

  // Custom decoders for circe
  // Forced to use "for" loops for the decoders. All the documentation I found uses them.
  implicit val nodeObjectDecoder: Decoder[NodeObject] = new Decoder[NodeObject] {
    final def apply(c: HCursor): Decoder.Result[NodeObject] =
      for {
        id <- c.downField("id").as[Int]
        children <- c.downField("children").as[Int]
        props <- c.downField("props").as[Int]
        currentDepth <- c.downField("currentDepth").as[Int]
        propValueRange <- c.downField("propValueRange").as[Int]
        maxDepth <- c.downField("maxDepth").as[Int]
        maxBranchingFactor <- c.downField("maxBranchingFactor").as[Int]
        maxProperties <- c.downField("maxProperties").as[Int]
        storedValue <- c.downField("storedValue").as[Double]
        valuableData <- c.downField("valuableData").as[Boolean]
      } yield NodeObject(id, children, props, currentDepth, propValueRange,
        maxDepth, maxBranchingFactor, maxProperties, storedValue, valuableData)
  }

  implicit val actionObjectDecoder: Decoder[Action] = new Decoder[Action] {
    final def apply(c: HCursor): Decoder.Result[Action] =
      for {
        actionType <- c.downField("actionType").as[Int]
        fromNode <- c.downField("fromNode").as[NodeObject]
        toNode <- c.downField("toNode").as[NodeObject]
        fromId <- c.downField("fromId").as[Int]
        toId <- c.downField("toId").as[Int]
        resultingValue <- c.downField("resultingValue").as[Option[Int]]
        cost <- c.downField("cost").as[Double]
      } yield Action(actionType = actionType, fromNode = fromNode, toNode = toNode, fromId = fromId, toId = toId, resultingValue = resultingValue, cost = cost)
  }

  def loadGraph(dir: String, fileName: String): Option[NetGraph] = {
    // Using Hadoop FileSystem and InputStream for both AWS & Local Machine
    val conf = new Configuration()
    //    AWS EMR
    //    val fileSystem = FileSystem.get(java.net.URI.create(dir), conf)
    val fileSystem = FileSystem.get(conf)
    val fsDataInputStream = fileSystem.open(new Path(dir.concat(fileName)))
    // Loading json and converting them to strings
    val json = Source.fromInputStream(fsDataInputStream).mkString
    val arr = json.split(NGSConstants.NEW_LINE)
    // After splitting the strings, we initialize node & edge arrays and return them
    val nodeArr = decode[Set[NodeObject]](arr.head).right.get.toList
    val edgeArr = decode[Set[Action]](arr.last).right.get.toList

    NetModelAlgebra(nodeArr, edgeArr)
  }
}