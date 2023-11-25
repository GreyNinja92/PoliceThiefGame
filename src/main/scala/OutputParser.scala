import NetGraphAlgebraDefs.NodeObject

import scala.collection.mutable.ArrayBuffer
import scala.jdk.CollectionConverters._
import scala.io.Source
import java.io.{File, PrintWriter}
import scala.collection.mutable
import scala.collection.mutable.{ArrayBuffer, HashSet, ListBuffer}
import org.yaml.snakeyaml.Yaml

object OutputParser {
  val oNodeAdded = ArrayBuffer[Int]()
  val oNodeRemoved = ArrayBuffer[Int]()
  val oNodeModified = ArrayBuffer[Int]()
  val oEdgesAdded = ArrayBuffer[String]()
  val oEdgesRemoved = ArrayBuffer[String]()
  val oEdgesModified = ArrayBuffer[String]()

  private def edgesHelper(edges: Option[java.util.Map[Integer, Integer]]): Array[String] = {
    val edgeArr = ArrayBuffer[String]()
    if(edges.isEmpty) edgeArr.toArray
    else {
      edges.get.keySet().forEach { key => {
        edgeArr += s" ${key.toString}:${edges.get.get(key).toString} "
      }}
      edgeArr.toArray
    }
  }

  def isNodeModified(node: NodeObject): Boolean = {
    oNodeAdded.contains(node.id) || oNodeModified.contains(node.id) || oNodeRemoved.contains(node.id)
  }

  def parseGoldenYAML(fileName: String): Unit = {
//    logger.info("Parsing the original yaml file")
    val source = Source.fromFile("difference.yaml")
    val lines = source.getLines().mkString("\n").replace("\t", " " * 4)

    val yamlString = ""
    val yaml = new Yaml()
    val data = yaml.load(lines).asInstanceOf[java.util.Map[String, Any]]
    val nodes = data.get(NGSConstants.NODES).asInstanceOf[java.util.Map[String, Any]]
    val edges = data.get(NGSConstants.EDGES).asInstanceOf[java.util.Map[String, Any]]

    if(nodes.get(NGSConstants.MODIFIED) != null) nodes.get(NGSConstants.MODIFIED).asInstanceOf[java.util.ArrayList[Integer]].forEach { ele => oNodeModified += ele }
    if(nodes.get(NGSConstants.REMOVED) != null) nodes.get(NGSConstants.REMOVED).asInstanceOf[java.util.ArrayList[Integer]].forEach { ele => oNodeRemoved += ele }
    if(nodes.get(NGSConstants.ADDED) != null)  nodes.get(NGSConstants.ADDED).asInstanceOf[java.util.Map[Integer, Integer]].values().forEach { ele => oNodeAdded += ele }

    oEdgesModified.addAll(edgesHelper(Option(edges.get(NGSConstants.MODIFIED).asInstanceOf[java.util.Map[Integer, Integer]])))
    oEdgesAdded.addAll(edgesHelper(Option(edges.get(NGSConstants.ADDED).asInstanceOf[java.util.Map[Integer, Integer]])))
    oEdgesRemoved.addAll(edgesHelper(Option(edges.get(NGSConstants.REMOVED).asInstanceOf[java.util.Map[Integer, Integer]])))

    println("BOOM")
    println(oNodeAdded)
    println(oNodeModified)
    println(oNodeRemoved)
    println(oEdgesAdded)
    println(oEdgesModified)
    println(oEdgesRemoved)
  }
}