package rl;

public class RLParamNames {

	// experiment parameters
	public static final String EPISODES = "episodes";
	public static final String GAME_DURATION = "game-duration";
	public static final String ABSTRACTION_MODEL = "abstraction-model";
	public static final String REWARD_FUNCTION = "reward-function";
	public static final String OUTPUT_DIR = "output-dir";
	public static final String QUIET_LEARNING = "quiet-learning";	// don't output knowledge every episode
	public static final String MICRORTS_OPPONENT = "microrts-opponent"; //the opponent in SingleAgent model
	
	// parameters of RL methods
	public static final String DISCOUNT = "discount";
	public static final String LEARNING_RATE = "learning-rate";
	public static final String INITIAL_Q = "initial-q";
	public static final String EPSILON = "epsilon";
	public static final String DUMMY_POLICY = "dummy-policy";
	public static final String PATH_TO_KNOWLEDGE = "path-to-knowledge";
	
	// parameters of search methods
	public static final String TIMEOUT = "timeout";
	public static final String PLAYOUTS = "playouts";
	public static final String LOOKAHEAD = "lookahead";
	public static final String EVALUATION_FUNCTION = "evaluation-function"; 
	
	
		
	public static final String PLAYERS = "players";	//does not appear in XML
	
	public static final String DEBUG_LEVEL = "debug-level";	// how verbose should we be?
	
	// command line only: 
	public static final String CONFIG_FILE = "config-file";
	public static final String PLAYER1_POLICY = "player1policy";
	public static final String PLAYER2_POLICY = "player2policy";
	
}
