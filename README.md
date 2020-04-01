## micrortsFA
Temporal Difference Function Approximation AI implementation in microRTS (Santiago Ontañón (2013) The Combinatorial Multi-Armed Bandit Problem and its Application to Real-Time Strategy Games, In AIIDE 2013. pp. 58 - 64.)

This was built over the BURLAP (http://burlap.cs.brown.edu/) integrated microRTS version for Reinforcement Learning from https://github.com/amandaccsantos/microrts (A. TAVARES, H. AZPÚRUA, A. SANTOS, AND L. CHAIMOWICZ, Rock, Paper, Star-
Craft: Strategy Selection in Real-Time Strategy Games, in 12th Artificial Intelligence and Interactive Digital Entertainment Conference (AIIDE), 2016, pp. 93–99.) 

## Instructions:
### To reproduce Tavares et al. IJCAI 2018 experiments:

This example contains the parameters against PuppetSearchMTCS. Configure a xml like below:

```xml
<experiment> <!-- Configured according to Tavares et al. IJCAI 2018 -->
	<parameters>
		<episodes value="500" /> <!-- number of matches -->
		<game-duration value="3000" /> <!-- game duration in cycles -->
		<abstraction-model value="singleagent" /> <!-- the opponent is embedded in the environment -->
		<microrts-opponent value="PuppetSearchMCTS" /> <!-- this is the opponent embedded in the environment -->
		<output-dir value="/tmp/results" />
		<reward-function value="winloss" /> <!-- win=1, loss=-1, draw=0 -->
		<quiet-learning value="false" /> <!-- quiet=false makes it outputs the knowledge after every episode -->
		<debug-level value="0" />
	</parameters>
	
	<player name='MetaBot' type='MetaBotAIAdapterLQ'>
		<!-- <path-to-knowledge value="policy/q_MetaBot_final.txt"></path-to-knowledge> (used for testing, I think) -->
		<learning-rate-meta type="exponential-decay" initial="1E-4" final="1E-4" rate="1" /> <!-- Remain constant at 1E-4 -->
		<discount value="0.9" /> <!-- discount factor for future rewards (gamma) -->
		<epsilon value="0.2" /> <!-- initial exploration rate (epsilon) -->
		<decay-rate value="0.9984" /> <!-- decay rate of epsilon -->		
	</player>
	
	<player name='pgsai' type='Dummy'> <!-- Dummy because the second player is embedded in the environment -->
			<dummy-policy value="WorkerRush" /> <!-- does not matter -->
	</player>

</experiment>
```

Let's assume it is saved at `<config-path>`. Then, run with:

`python experimentmanagerRestartable.py -c <config-path> -o OUT -n N -s S`

Replace OUT by the output directory, N by the total number of repetitions and S by the number of repetitions running simultaneously (in parallel). Data for each repetition will be saved at `OUT/repNUM`, where NUM is in range(0, N).

As mentioned, the config above is for PuppetMCTS. To play against other opponents, replace the values in `microrts-opponent`, `episodes value`, `epsilon`, `decay-rate` and `learning-rate-meta` tags to the ones in the paper: 

"alpha = 1E-4 , gamma = 0.9, epsilon exponentially decaying from 0.2 against PuppetAB, PuppetMCTS and AHTN; and decaying from 0.1 for NaiveMCTS and StrategyTactics, after every game (decay rate ≈ 0.9984). All games have 3000 cycles."

"500 games against PuppetAB, PuppetMCTS and AHTN; and in 100 games against NaiveMCTS and StrategyTactics."

