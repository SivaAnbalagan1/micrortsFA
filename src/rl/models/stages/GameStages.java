package rl.models.stages;

public enum GameStages{
	OPENING, 	//0 - 599 frames
	EARLY,		//600 - 1199
	MID,		//1200 - 1799
	LATE,		//1800 - 2399
	END,		//2400 - 3000
	FINISHED	//whenever the game is over
}
