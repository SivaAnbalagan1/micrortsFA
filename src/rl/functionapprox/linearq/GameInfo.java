package rl.functionapprox.linearq;

import java.util.ArrayList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rts.GameState;
import rts.PhysicalGameState;
import rts.units.Unit;
import rts.units.UnitType;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

public class GameInfo {
	PhysicalGameState pgs;
	private int player;
	String name,quad;
	int quadnos;
	ArrayList<String> unitTypes;
	ArrayList<String> quadrants;//Quadrant names
	Map<String,Map> feature = new HashMap();//For each quadrant, unit types and count
	Map<String,Map> featureOpp = new HashMap();//For each quadrant, unit types and count
	Map<String,Double> featureHealth = new HashMap();//Health of player units at each quad
	Map<String,Double> featureHealthOpp = new HashMap();//Health of opponent units at each quad
	Map<String,Double> unitCount,unitCounttemp;//Unit types and their count.
	Map<String, Double> unitCountP1,unitCountP2;

	
	//Map<String,Map<String,Map<String,Double>>> actionUnitsweightsP,actionUnitsweightsO;
	String[] ainames;
	double [] featureValue,weights;
    ArrayList<double []> gameValues;
    List<UnitType> unitTypeList;
    SummaryStatistics stat = new SummaryStatistics();
    SummaryStatistics stat_time = new SummaryStatistics();
    
	public GameInfo(GameState gs,String[] ai, List<UnitType> lu,int playr) {
		unitTypes = new ArrayList<String>();
		quadrants = new ArrayList<String>();
		feature = new HashMap();featureOpp = new HashMap();
		featureHealth = new HashMap();featureHealthOpp = new HashMap();
		unitCount = new HashMap();unitCounttemp = new HashMap();
		unitCountP1 = new HashMap(); unitCountP2=new HashMap();

		ainames = ai;	unitTypeList =lu;
		this.player = playr; 
		for(UnitType u : unitTypeList)unitTypes.add(u.name);
		quadrants();
	}
	@SuppressWarnings("unchecked")
	public double [] findGamePosition(GameState gs){
		int add;
		double hitpointPrev,unitnum,value;
		pgs = gs.getPhysicalGameState();
		buildFeatureSpace();
		
		for (Unit u : pgs.getUnits()){
			if(!u.getType().isResource){//resources are neutral. player value -1.
			name = u.getType().name;
			quad = findQuadrant(u.getX(),u.getY());
			if(u.getType().isStockpile)add = u.getResources();//adds up stock pile res
			else add =1;

			if(u.getPlayer()==player){
				unitCounttemp = feature.get(quad);
				double count = unitCounttemp.get(name) + add;
//				System.out.println(" type" + u.getType().name + " value "+ add + " " + count);
				unitCounttemp.put(name, count);//update Unit count
				feature.put(quad,unitCounttemp);//update for respective quadrant
				//health update	
				hitpointPrev = (double)featureHealth.get(quad);
				unitnum = unitCountP1.get(quad);
				value = (hitpointPrev * (unitnum-1)+u.getHitPoints())/(unitnum+1);//moving avg
				featureHealth.put(quad, value);
				unitCountP1.put(quad, unitnum+1);//update unit counts in this quad
			}
			else{
				
				unitCounttemp = featureOpp.get(quad);
				Double count = unitCounttemp.get(name) + add;
				unitCounttemp.put(name, count);
				featureOpp.put(quad,unitCounttemp);
			//health update	
				hitpointPrev = featureHealthOpp.get(quad);
				unitnum = unitCountP2.get(quad);
				value = (hitpointPrev * (unitnum-1)+u.getHitPoints())/(unitnum+1);//moving avg
				featureHealthOpp.put(quad, value);
				unitCountP2.put(quad, unitnum+1);//update unit counts in this quad
			}
			}
		}
		setupFeatnWeight();
		standardizeTimeFeat(gs.getTime());
		normalize();
		featureValue[featureValue.length-1] = 1;
/*		for(int j=0;j<featureValue.length;j++){
			System.out.print(featureValue[j]+ " ");
		}System.out.println();*/
	//	featureValue[featureValue.length-1] = gs.getTime();
		
/*		for(int i=0;i<featureValue.length;i++){
			System.out.print(featureValue[i]);
		}			System.out.println(" ");
*/
		//gameValues.clear();
		//gameValues.add(featureValue);gameValues.add(weights);
		return featureValue;
	}
	public void buildFeatureSpace(){
		//For each player adding unit count map for each game quadrant
//		feature.clear();featureOpp.clear();unitCount.clear();
		for(UnitType u : unitTypeList){//add other than resources fo the players
			if(!u.isResource)unitCount.put(u.name, 0.0);
		}
		for(int i=1;i<=3;i++)
			for(int j=1;j<=3;j++){
				feature.put(Integer.toString(i)+Integer.toString(j),new HashMap<String, Double>(unitCount));
				featureOpp.put(Integer.toString(i)+Integer.toString(j), new HashMap<String, Double>(unitCount));
		  	   featureHealth.put(Integer.toString(i)+Integer.toString(j), 0.0);
				featureHealthOpp.put(Integer.toString(i)+Integer.toString(j), 0.0);
				unitCountP1.put(Integer.toString(i)+Integer.toString(j), 0.0);
				unitCountP2.put(Integer.toString(i)+Integer.toString(j), 0.0);

			}
	}
	
	public void setupFeatnWeight(){
		int i;
		int runit,featsize=0,quadfeat,runitname;
		runit=0;runitname=0;
		for (Unit u : pgs.getUnits())if(u.getType().isResource)runit++;
		for(UnitType ut: unitTypeList)if(ut.isResource)runitname++;
		quadfeat = unitTypeList.size()-runitname;
		featsize = quadfeat *9*2+9+9+2+1+1;//9-health-2,total res,1-time, intercept
	//	featsize = quadfeat *9*2+1;//1-time
	//	System.out.println(quadfeat + " " +runit + " " + featsize);
	//	weights = new double[unitTypes.size() *quadnos*2*ainames.length];
		featureValue = new double[featsize];
		i=0;
		for(String qd: quadrants)
			for(String type: unitTypes){
				if(type!="Resource"){
				featureValue[i] = (double) feature.get(qd).get(type);
				stat.addValue(featureValue[i]);
				i++;
				}
			}
		//System.out.println( "ind " +i );
		for(String qd: quadrants)
			for(String type: unitTypes){
				if(type!="Resource"){
				featureValue[i] =  (double) featureOpp.get(qd).get(type);
				stat.addValue(featureValue[i]);
				i++;}
			}//System.out.println( "ind " +i );
	//additional feature begin
		for(String qd: quadrants){
			featureValue[i] = (double)featureHealth.get(quad);
			stat.addValue(featureValue[i]);
			i++;
		}
		for(String qd: quadrants){
			featureValue[i] = (double )featureHealthOpp.get(quad);
			stat.addValue(featureValue[i]);
			i++;
		}
	
		featureValue[i] = pgs.getPlayer(0).getResources();
			stat.addValue(featureValue[i]);i++;
		featureValue[i] = pgs.getPlayer(1).getResources();
			stat.addValue(featureValue[i]);
	//additional feature end	
	/*	for (Unit u : pgs.getUnits()){
			if(u.getType().isResource){
				featureValue[i]=u.getResources();
				stat.addValue(featureValue[i]);
				i++;
			}
		}*/
		
		/*for(int j=0;j<featureValue.length;j++){
			System.out.print(featureValue[j]+ " ");
		}System.out.println();*/
	/*	i=0;
		for(String action: ainames)
			for(String qd: quadrants)
				for(String type: unitTypes){
					weights[i] = actionUnitsweightsP.get(action).get(qd).get(type);
					i++;
				}
		for(String action: ainames)
			for(String qd: quadrants)
				for(String type: unitTypes){
					weights[i] = actionUnitsweightsO.get(action).get(qd).get(type);
					i++;
				}

		*/
	}

	public String findQuadrant(int widthpos, int heightpos){
		 String quadrant = " ";
		 quadnos = 9;
		 if (widthpos <=21)quadrant = "1";		 
		 else if (widthpos <=42)quadrant = "2";		 
		 else if (widthpos <=64)quadrant = "3";

		 if (heightpos <=21)quadrant += "1";
		 else if (heightpos <=42)quadrant += "2";
		 else if (heightpos <=64)quadrant += "3";
        return quadrant;
	}
	
	
	public void quadrants(){
	//	player="p";	
		for(int i=1;i<=3;i++)
			for(int j=1;j<=3;j++)
				quadrants.add(Integer.toString(i)+Integer.toString(j));
	/*	player="o";	
		for(int i=1;i<=6;i++)
			for(int j=1;j<=6;j++)
				quadrants.add(player+Integer.toString(i)+Integer.toString(j));*/
		
	}
	public void normalize(){
		//Data Normalization.
		double avg = stat.getMean();
		double std = stat.getStandardDeviation();
		double max = stat.getMax();
		double min = stat.getMin();
		for(int i=0;i<featureValue.length;i++){
			featureValue[i] = (featureValue[i]-avg)/(max-min);
//			System.out.println("norm - " + featureValue[i] + " i ");
		}

	}
	public void standardizeTimeFeat(int time){
		//Standardisation for streaming data. 
		//Range wont be available for this to calculate normalised
		for(int i=0;i<3000;i++){
			stat_time.addValue(i);
		}
		
		double avg = stat_time.getMean();
		double std = stat_time.getStandardDeviation();
		featureValue[featureValue.length-1] = (time-avg)/std;
	}

}
