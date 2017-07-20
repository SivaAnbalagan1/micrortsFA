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
	Map<String,Double> unitCount,unitCounttemp;//Unit types and their count.
	
	//Map<String,Map<String,Map<String,Double>>> actionUnitsweightsP,actionUnitsweightsO;
	String[] ainames;
	double [] featureValue,weights;
    ArrayList<double []> gameValues;
    List<UnitType> unitTypeList;
    SummaryStatistics stat = new SummaryStatistics();
    
	public GameInfo(GameState gs,String[] ai, List<UnitType> lu) {
		unitTypes = new ArrayList<String>();
		quadrants = new ArrayList<String>();
		feature = new HashMap();featureOpp = new HashMap();
		unitCount = new HashMap();unitCounttemp = new HashMap();
		ainames = ai;	unitTypeList =lu;
		 
		for(UnitType u : unitTypeList)unitTypes.add(u.name);
		
		quadrants();
	}
	@SuppressWarnings("unchecked")
	public double [] findGamePosition(GameState gs){
		
		pgs = gs.getPhysicalGameState();
		buildFeatureSpace();
		
		for (Unit u : pgs.getUnits()){
			name = u.getType().name;
			quad = findQuadrant(u.getX(),u.getY());
			if(u.getPlayer()==player){
				unitCounttemp = feature.get(quad);
				Double count = unitCounttemp.get(name) + 1.0;
				unitCounttemp.put(name, count);//update Unit count
				feature.put(quad,unitCounttemp);//update for respective quadrant
			}
			else{
				unitCounttemp = featureOpp.get(quad);
				Double count = unitCounttemp.get(name) + 1.0;
				unitCounttemp.put(name, count);
				featureOpp.put(quad,unitCounttemp);

			}
		}
		setupFeatnWeight();
		featureValue[featureValue.length-1] = gs.getTime();
		normalize();
		//gameValues.clear();
		//gameValues.add(featureValue);gameValues.add(weights);
		return featureValue;
	}
	public void buildFeatureSpace(){
		//For each player adding unit count map for each game quadrant
//		feature.clear();featureOpp.clear();unitCount.clear();
		for(UnitType u : unitTypeList)unitCount.put(u.name, 0.0);
		for(int i=1;i<=3;i++)
			for(int j=1;j<=3;j++){
				feature.put(Integer.toString(i)+Integer.toString(j), unitCount);
				featureOpp.put(Integer.toString(i)+Integer.toString(j), unitCount);
			}
	}
	
	public void setupFeatnWeight(){
		int i;
	//	weights = new double[unitTypes.size() *quadnos*2*ainames.length];
		featureValue = new double[unitTypes.size() *quadnos*2+1];
		i=0;
		for(String qd: quadrants)
			for(String type: unitTypes){
				featureValue[i] = (double) feature.get(qd).get(type);
				stat.addValue(featureValue[i]);
				i++;
			}
		for(String qd: quadrants)
			for(String type: unitTypes){
				featureValue[i] = (double) featureOpp.get(qd).get(type);
				stat.addValue(featureValue[i]);
				i++;
			}
		
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
		 if (widthpos <=8)quadrant = "1";		 
		 else if (widthpos <=16)quadrant = "2";		 
		 else if (widthpos <=24)quadrant = "3";

		 if (heightpos <=8)quadrant += "1";
		 else if (heightpos <=16)quadrant += "2";
		 else if (heightpos <=24)quadrant += "3";
		 
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
		}

	}

}
