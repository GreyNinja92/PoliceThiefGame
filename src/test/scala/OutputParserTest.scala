import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.mockito.Mockito.{verify}
import org.scalatestplus.mockito.MockitoSugar
import org.yaml.snakeyaml.Yaml

class OutputParserTest extends AnyFlatSpec with Matchers with MockitoSugar {

  it should "read YAML" in {
    val yaml = mock[Yaml]
    OutputParser.parseGoldenYAML("difference.yaml")

    verify(yaml).load(_: String)
  }
}
