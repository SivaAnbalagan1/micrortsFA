package rl;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import burlap.behavior.stochasticgames.GameEpisode;
import burlap.mdp.stochasticgames.world.World;
import rl.adapters.learners.PersistentLearner;

public class RLStateStore implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private Map<String, Object> parameters;
	private boolean quietLearning;
	private World gameWorld;
	private List<PersistentLearner> agents;
	private String outDir;
	private int numEpisodes,episodenum;
	private GameEpisode episode;
	
	public RLStateStore() {
		// TODO Auto-generated constructor stub
	}
	public void setValues(Map<String, Object> p,boolean q, World g,
			List<PersistentLearner> ag,	int n,String o,GameEpisode e,int epi){
		parameters = p;
		quietLearning =q;
		gameWorld =g;
		agents = ag; numEpisodes = n; episode =e;outDir=o;episodenum=epi;
	}
	public Map<String, Object> getparams(){return parameters;}
	public boolean getquietLearning(){return quietLearning;}
	public World getgameWorld(){return gameWorld;}
	public List<PersistentLearner> getagents(){return agents;}
    public String getoutDir(){return outDir;}
    public int getnumEpisodes(){return numEpisodes;}
    public GameEpisode getepisode(){return episode;}
    public int getepisodecnt(){return episodenum;}
}
