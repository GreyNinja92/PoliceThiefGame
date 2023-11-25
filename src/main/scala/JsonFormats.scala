import spray.json.RootJsonFormat
import spray.json.DefaultJsonProtocol
import spray.json.DefaultJsonProtocol._//{IntJsonFormat, StringJsonFormat, jsonFormat1, jsonFormat2}

object JsonFormats {
  implicit val policeJsonFormat: RootJsonFormat[Police] = jsonFormat2(Police.apply)
  implicit val defResponseJsonFormat: RootJsonFormat[DefResponse] = jsonFormat1(DefResponse.apply)
  implicit val nextMovesJsonFormat: RootJsonFormat[NextMoves] = jsonFormat1(NextMoves.apply)
}
