// Stores constants for the entire application
object NGSConstants {
  val SERVER = "GameServer"
  val ORIGINAL_GRAPH = "original.json"
  val PERTURBED_GRAPH = "perturbed.json"
  val DIFFERENCE_YAML = "difference.yaml"

  val NODES: String = "Nodes"
  val EDGES: String = "Edges"
  val NEW_LINE = "\n"

  val POLICE_ACTOR = "policeActor"
  val THIEF_ACTOR = "thiefActor"

  val AWS_URL = "0.0.0.0"
  val URL = "localhost"

  val THIEF = "Thief"
  val POLICE = "Police"

  val SAFE = "safe"
  val RANDOM = "random"

  val ADDED: String = "Added"
  val MODIFIED: String = "Modified"
  val REMOVED: String = "Removed"
  val PERTURBED = ".perturbed"
  val OUTPUTDIRECTORY = "output"

  val INITIALIZED_THIEF = "Initialized Thief"
  val INITIALIZED_POLICE = "Initialized Police"

  val POLICE_FOUND_THIEF = "Police found thief. Police wins"
  val THIEF_FOUND_DATA = "Thief found valuable data. Thief wins"

  val MOVED_POLICE = "Moved police to position"
  val MOVED_THIEF = "Moved thief to position"
  val FAILED_TO_MOVE = "Failed to move to position"

  val POLICE_TURN = "Police's turn"
  val THIEF_TURN = "Thief's turn"

  val ENTER_VALID_STRATEGY = "Enter a valid strategy"
  val STILL_PLAYING_GAME = "Still playing game"
  val SET_STRATEGY_FIRST = "Set a strategy first"

  val RESET_GAME = "Reset game"

  val PATH_POLICE = "police"
  val PATH_THIEF = "thief"
  val POSSIBLE_MOVES = "possibleMoves"
  val FIND_VALUABLE_NODE = "findValuableNode"
  val FIND_THIEF = "findThief"
  val FIND_POLICE = "findPolice"
  val RESULT = "result"
  val MOVE = "move"
  val STRATEGY = "strategy"
  val RESTART = "restart"

  def OTHER_PLAYER_LOCATION(id: Int, player: String): String = s"${player} is at node ${id}"

  def PLAYING_WITH_STRATEGY(str: String): String = s"Playing with strategy: ${str}"

  def MOVE_CANNOT_BE_PERFORMED(player: String): String = s"Move cannot be performed. ${player} loses"
  def DISTANCE_FROM_NODE(dist: Int): String = s"Distance from valuable node : ${dist}"

  def NO_MOVES_AVAILABLE(player: String): String = s"No moves available for ${player}. ${player} loses."
}
