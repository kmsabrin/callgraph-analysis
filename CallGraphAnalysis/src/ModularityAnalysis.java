import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.math3.stat.StatUtils;

public class ModularityAnalysis {
	
	Map<String, Set<String>> communities;
	Map<String, Double> communitiesInDeg;
	Map<String, Double> communitiesOutDeg;
	int nCommunityNetoworkEdge;
	static int nCommunitySizeThreshold = 5;
	
	public void getModuleGeneralityVsComplexity(CallDAG callDAG, String filePath) throws Exception {
		PrintWriter pw = new PrintWriter(new File("Results//module-gen-vs-cmp-" + filePath + ".txt"));
		for (String s: callDAG.functions) {
			pw.println(callDAG.moduleGenerality.get(s) + "\t" + callDAG.moduleComplexity.get(s));
		}
		pw.close();
	}
	
	public void getAvgModuleGeneralityVsLocation(CallDAG callDAG, String filePath) throws Exception {
		PrintWriter pw = new PrintWriter(new File("Results//avg-module-gen-vs-loc-" + filePath + ".txt"));		
		Map<Double, Double> avgModuleGeneralityVsLocation = new TreeMap();
		for (String s: callDAG.functions) {
			double loc = callDAG.location.get(s);
			double mG = callDAG.moduleGenerality.get(s);
			
			if ( avgModuleGeneralityVsLocation.containsKey(loc) ) {
				double oldMG = avgModuleGeneralityVsLocation.get(loc);
				avgModuleGeneralityVsLocation.put(loc, (mG + oldMG) / 2.0);
			}
			else {
				avgModuleGeneralityVsLocation.put(loc, mG);
			}
		}
		
		for (double l: avgModuleGeneralityVsLocation.keySet()) {
			pw.println(l + "\t" + avgModuleGeneralityVsLocation.get(l));
		}
		
		pw.close();
	}
	
	public void doCentralityAnalysisWithModuleGenerality() throws Exception {
		PrintWriter pw = new PrintWriter(new File("Results//module-core-percentage.txt"));

		for (int i = Driver.versiontStart; i < Driver.versionEnd; ++i) {
			String versionNum = Driver.networkUsed + i;
			CallDAG callDAG = new CallDAG(Driver.networkPath + i);
			
			double a[] = new double[callDAG.moduleGenerality.size()];
			int index  = 0;
			for (String s: callDAG.moduleGenerality.keySet()) {
				a[index++] = callDAG.moduleGenerality.get(s);
			}
	
			double coreCutOff = StatUtils.percentile(a, 75.0);
			
			double coreCount = 0;
			PrintWriter pw2 = new PrintWriter(new File("Results//module-core-location-hist-" + versionNum + ".txt"));
			for (String s: callDAG.functions) {
				if (callDAG.moduleGenerality.get(s) > coreCutOff) {
					// core node
					++coreCount;
					pw2.println(callDAG.location.get(s));
				}
			}
			
			pw2.close();
			
			pw.println(coreCount / (callDAG.functions.size() * 1.0));
		}
		
		pw.close();
	}
	
	
	/* GENERATE RANDOM MODULAR NETWORK TO TEST WALK-TRAP COMMUNITY DETECTION ALGORITHM'S EFFECTIVENESS */
	
	public void getRandomModularNetwork() throws Exception {
		PrintWriter pw = new PrintWriter(new File("Results//random-modular-network_100x100.txt"));

		HashMap<Integer, HashSet<Integer>> adjacencyList = new HashMap();
		
		int blockSize = 100;
		int nBlocks = 100;
		int nNodes = blockSize * nBlocks;
				
		for (int i = 0; i < nNodes; ++i) {
			HashSet<Integer> hset = new HashSet();
			adjacencyList.put(i, hset);
		}
		
		// create intra-module edges
		int kount;
		for (int i = 0; i < nBlocks; ++i) {
			Random rand = new Random(System.nanoTime());
			kount = (int)(blockSize * 2);
			while (kount-- > 0) {
				int source = rand.nextInt(blockSize);
				int target = rand.nextInt(blockSize);
				if (source == target) continue;
				if (source > target) {
					int temp = source;
					source  = target;
					target = temp;
				}
				source += i * blockSize;
				target += i * blockSize;
				adjacencyList.get(source).add(target);
			}
		}
		
//		int[] layerWidths = new int[]{4, 2, 1, 3, 5, 5};
//		int[] layerWidths = new int[]{5, 5, 3, 2, 1, 4};
//		int[] layerWidths = new int[]{50, 30, 20, 20, 30, 50};
		int[] layerWidths = new int[]{25, 15, 10, 10, 15, 25};

		int[] layerPrecedence = new int[nNodes];

		int k = 0;
		int precedence = 1;
		for (int i: layerWidths) {
			for (int j = 0; j < i * blockSize; ++j) {
				layerPrecedence[k++] = precedence;
			}
			++precedence;
		}
		
		// create inter-module edges
		kount = nNodes / 5;
		Random rand = new Random(System.nanoTime());
		while (kount > 0) {
			int source = rand.nextInt(nNodes);
			int target = rand.nextInt(nNodes);			
			if (source / blockSize == target / blockSize) continue;
			if (layerPrecedence[source] > layerPrecedence[target]) continue;
			adjacencyList.get(source).add(target);
			kount--;
		}
		
		boolean[] notZeroDegree = new boolean[nNodes];
		
		for (int i = 0; i < nNodes; ++i) {
			for (int j: adjacencyList.get(i)) {
				pw.println(i + "  " + j);
				notZeroDegree[i] = true;
				notZeroDegree[j] = true;
			}
		}
		
		for (int i = 0; i < nNodes; ++i) {
			if (notZeroDegree[i] == false) {
				pw.println("0" + "  " + i);
			}
		}
		
		pw.close();
		
//		CallDAG callDAG = new CallDAG("Results//random-modular-network.txt");
	}
	
	
	/*********************************************************************************************************************/
	/************************************* WALK TRAP COMMUNITY DETECTION ALGORITHM ***************************************/
	/****************** ANALYSIS OF COMMUNITY NETWORK GENERATED WITH WALK TRAP ALGORITHM *********************************/
	/*********************************************************************************************************************/
	
	public void getWalktrapModules(CallDAG callDAG, String versionNum) throws Exception {	
		Scanner scanner = new Scanner(new File("module_graphs//w5-" + versionNum + ".txt"));
		PrintWriter pw = new PrintWriter(new File("Results//communities-" + versionNum + ".txt"));
		
		communities = new TreeMap();
		communitiesInDeg = new HashMap();
		communitiesOutDeg = new HashMap();
		
		int communityID = 1;
		while (scanner.hasNextLine()) {
			String str = scanner.nextLine();
			str = str.substring(str.indexOf('{') + 1, str.indexOf('}'));
			String val[] = str.split(",");	
		
			if (val.length < nCommunitySizeThreshold) continue;

			Set<String> communityFunctions = new HashSet();
			String cID = "C" + communityID;
			
			pw.print(cID);
			
			double avgModGen = 0;
			double avgLoc = 0;
			for(String r: val) {
				int id = Integer.parseInt(r);
				String f = callDAG.IDFunction.get(id);
				communityFunctions.add(f);
				pw.print("\t" + f);
				
				double loc = callDAG.location.get(f);
				double modGen = callDAG.generality.get(f); 
//				if (loc > 0.3 && loc < 0.5 && modGen > 0.1) {
//					System.out.print(f + "\t");
//				}
				avgModGen += modGen;
				avgLoc += loc;
			}
			System.out.print((avgModGen / communityFunctions.size()) + "\t" + (avgLoc / communityFunctions.size()));
			System.out.println("\t" + communityFunctions.size() + "\t" + cID);
			pw.println();
			
			communities.put(cID, communityFunctions);
			++communityID;
		}
		
		nCommunityNetoworkEdge = 0;
		for (String s: communities.keySet()) {
			Set<String> currentComm = communities.get(s);
			double inDeg = 0, outDeg = 0;
			for (String r: currentComm) {
				if (callDAG.callTo.containsKey(r)) {
					for (String t: callDAG.callTo.get(r)) {
						if (!currentComm.contains(t)) ++outDeg;
					}
				}
				if (callDAG.callFrom.containsKey(r)) {
					for (String t: callDAG.callFrom.get(r)) {
						if (!currentComm.contains(t)) ++inDeg;
					}
				}
			}
			communitiesInDeg.put(s, inDeg);
			communitiesOutDeg.put(s, outDeg);
			nCommunityNetoworkEdge += outDeg;
		}

		System.out.println("nCommunities: " + (communityID - 1));
		scanner.close();
		pw.close();
	}
	
	public double getCommunityDistanceMetric(String comXID, String comYID, CallDAG callDAG) {
		Set<String> comX = communities.get(comXID);
		Set<String> comY = communities.get(comYID);
		
		double nComXOutgoingEdge = communitiesOutDeg.get(comXID);
		double nComYIncomingEdge = communitiesInDeg.get(comYID);
		double nComYOutgoingEdge = communitiesOutDeg.get(comYID);
		double nComXToComYEdge = 0;
		
		double dist = 0;

		for (String s: comX) {
			if (!callDAG.callTo.containsKey(s)) continue; // why this is not handled by Java?			
			for (String r: callDAG.callTo.get(s)) {				
				if (comY.contains(r)) ++nComXToComYEdge;
			}
		}
		
		if (nComXOutgoingEdge > 0 && nComYIncomingEdge > 0) {
			double expecedtedXYEdge = nComXOutgoingEdge * nComYIncomingEdge / (nCommunityNetoworkEdge - nComYOutgoingEdge); 
			dist = nComXToComYEdge / expecedtedXYEdge;		
//			dist = nComXToComYEdge / (nComXOutgoingEdge * nComYInComingEdge);
		}
		
//		return dist * callDAG.nEdges;
		return dist;
	}
	
	public void getCommunityNetworkHeatMap(CallDAG callDAG) throws Exception {
		PrintWriter pw = new PrintWriter(new File("Results//communities_heat_map.txt"));
		
		for (String s: communities.keySet()) {
			for (String r: communities.keySet()) {
				double d = 0;
				if (!s.equals(r)) {
					d = getCommunityDistanceMetric(s, r, callDAG);
				}
				pw.print(String.format( "%.4f", d ) + "\t");
			}			
			pw.println();
		}
		
		pw.close();
	}
	
	public void getCommunityNetworkLayoutDOTStyle(CallDAG callDAG) throws Exception {
		PrintWriter pw = new PrintWriter(new File("community-callDAG.dot"));
		pw.println("digraph G {");
		
		Map<String, Double> communityGenerality = new TreeMap();
		
		for (String s: communities.keySet()) {
			double d = 0;
			for (String r: communities.keySet()) {
				if (!s.equals(r)) {
					d = getCommunityDistanceMetric(s, r, callDAG);
					
					if (communityGenerality.containsKey(r)) {
						double v = communityGenerality.get(r) + d;
						communityGenerality.put(r, v);
					}
					else {
						communityGenerality.put(r, d);
					}
				}
				else d = 0;

				if (d > 0.0009) {
					pw.println(s + " -> " + r + "[label=" + String.format( "%.4f", d ) + "];");
				}
			}
		}
		
		for (String s: communityGenerality.keySet()) {
			System.out.println(s + "\t in-weight " + communityGenerality.get(s));
		}
		
		pw.println("}");
		pw.close();
	}
		
	public void getCommunityLocationHistogram(CallDAG callDAG, String versionNum) throws Exception {		
		Scanner scanner = new Scanner(new File("module_graphs//w10-" + versionNum + ".txt"));
		PrintWriter pw = new PrintWriter(new File("Results//com_loc_histo.txt"));

		int commID = 0;
		
		List<String> communityList = new ArrayList();
		
		while (scanner.hasNextLine()) {
			String str = scanner.nextLine();
			str = str.substring(str.indexOf('{') + 1, str.indexOf('}'));
			communityList.add(str);
		}
		
		Collections.sort(communityList, new Comparator<String>() {
			public int compare(String left, String right) {
				return right.length() - left.length();
			}
		});
		
		double yStep = 1;
		
		for (String str: communityList) {
			String val[] = str.split(",");	
			double locations[] = new double[val.length];		
			if (locations.length < nCommunitySizeThreshold) continue;

			Map<Integer, Double> locationCounter = new TreeMap();
			int i = 0;
			double maxHeight = 0;
			for(String r: val) {
				int id = Integer.parseInt(r);
				String f = callDAG.IDFunction.get(id);
				int l = (int)(callDAG.location.get(f) * 100) / 10;
				
				locations[i++] = callDAG.location.get(f);
				
				double v = 0.0;
				if (locationCounter.containsKey(l)) {
					v = locationCounter.get(l) + 10;
					locationCounter.put(l, v);
				}
				else {
					locationCounter.put(l, v);
				}
				
				if (v > maxHeight) maxHeight = v;
				
				pw.println(l + "\t" + (yStep + v));
			}
					
			yStep += maxHeight + 500;
			commID++;
			
			pw.println("\n");
			
//			System.out.println(
//			  "Community" + commID		
//			+ "  Size: " + locations.length 	
//			+ "  Max: " + StatUtils.max(locations)
//			+ "  Min: " + StatUtils.min(locations)
//			+ "  Mean: " + StatUtils.mean(locations)
//			+ "  Median: " + StatUtils.percentile(locations, 50)
//			+ "  StdDev: " + Math.sqrt(StatUtils.variance(locations)));
		}
		
		System.out.println("nCommunities: " + commID);
		pw.close();
	}
	
	public void getDrawDataCommunityShape(CallDAG callDAG, List<String> communityList) throws Exception {
		PrintWriter pw = new PrintWriter(new File("Results//community_shape_javadraw.txt"));
		
		double xStep = 70; // java canvas
		
		boolean used[] = new boolean[communityList.size()];
		int notUsedKnt = communityList.size();
		
		while (notUsedKnt > 0) {
			boolean heightBits[] = new boolean[600];
			double maxRadius = 0;
			
			for (int idx = 0; idx < communityList.size(); ++idx) {
				if (used[idx]) continue;
				String str = communityList.get(idx);
				String val[] = str.split(",");
				double loc[] = new double[val.length];
				double gen[] = new double[val.length];

//				if (loc.length < nCommunitySizeThreshold)
//					continue;

				int i = 0;
				for (String r : val) {
					int id = Integer.parseInt(r);
					String f = callDAG.IDFunction.get(id);
					int l = (int) (callDAG.location.get(f) * 100) / 10;
					loc[i] = callDAG.location.get(f);
					gen[i] = callDAG.generality.get(f);
					++i;
				}

				double radius = Math.log10(loc.length) * 5.5; // HARD CODE
				// double radius = loc.length * 0.035;
				double xMid = xStep + radius;

				double yMin = StatUtils.percentile(loc, 75.0);
				double yMid = StatUtils.percentile(loc, 50.0);
				double yMax = StatUtils.percentile(loc, 25.0);
				yMin = 500 - ((yMin / 0.01) * 4.5);
				yMid = 500 - ((yMid / 0.01) * 4.5);
				yMax = 500 - ((yMax / 0.01) * 4.5);

				boolean flg = true;
				for (int k = (int)(yMid - radius); k <= (int)(yMid + radius); ++k) {
					if (heightBits[k]) {
						flg = false;
						break;
					}
				}
				if (!flg) continue;
				
				System.out.println("Using community " + notUsedKnt + " xStep " + xStep + " radius " + (int)radius + " yMid " + yMid);
				notUsedKnt--;
				used[idx] = true;
				for (int k = (int)(yMid - radius); k <= (int)(yMid + radius); ++k) {
					heightBits[k] = true;			
				}
				
				if (radius > maxRadius) maxRadius = radius;
				
				double avgGen = StatUtils.mean(gen);
//				if (avgGen < 0.02 && radius < 1.5)
//					continue; // HARD CODE

				if (loc.length < nCommunitySizeThreshold)
					continue;
				
				pw.print(xMid + "\t");
				pw.print(radius + "\t");

				pw.print(yMin + "\t");
				pw.print(yMid + "\t");
				pw.print(yMax + "\t");

				pw.print(avgGen + "\t");

				pw.print(loc.length);

				pw.println();
			}
			
			System.out.println("One stripe complete");
			xStep += 2 * maxRadius + 5;
		}
		
		pw.close();
	}
	
	public void getDrawDataCommunitySpread(CallDAG callDAG, List<String> communityList) throws Exception {
		PrintWriter pw = new PrintWriter(new File("Results//community_spread_javadraw.txt"));
		
		double xStep = 70; // java canvas
		
		for (int idx = 0; idx < communityList.size(); ++idx) {
			String str = communityList.get(idx);
			String val[] = str.split(",");	
			double loc[] = new double[val.length];	
			double gen[] = new double[val.length];
			
			if (loc.length < nCommunitySizeThreshold) continue;			

			int i = 0;
			
			
			for(String r: val) {
				int id = Integer.parseInt(r);
				String f = callDAG.IDFunction.get(id);
				int l = (int)(callDAG.location.get(f) * 100) / 10;
				loc[i] = callDAG.location.get(f);
				gen[i] = callDAG.generality.get(f);
				++i;
			}
			
			double radius = Math.log10(loc.length) * 1.926; //HARD CODE
//			double radius = loc.length * 0.035;
			double xMid = xStep + radius;

			double yMin = StatUtils.percentile(loc, 75.0);
			double yMid = StatUtils.percentile(loc, 50.0);
			double yMax = StatUtils.percentile(loc, 25.0);
			yMin = 500 - ((yMin / 0.01) * 4.5);
			yMid = 500 - ((yMid / 0.01) * 4.5);
			yMax = 500 - ((yMax / 0.01) * 4.5);
			
			double avgGen = StatUtils.mean(gen);
			
			if (avgGen < 0.02 && radius < 1.5) continue; // HARD CODE
			
			xStep = (xMid + radius) + 0.5;

			pw.print(xMid + "\t");
			pw.print(radius + "\t");

			pw.print(yMin + "\t");
			pw.print(yMid + "\t");
			pw.print(yMax + "\t");

			pw.print(avgGen + "\t");
			
			pw.print(loc.length);
			
			pw.println();
		}
		
		pw.close();
	}
	
	public void getCommunityAnalysisJavaDraw(CallDAG callDAG, String versionNum) throws Exception {		
		Scanner scanner = new Scanner(new File("module_graphs//w5-" + versionNum + ".txt"));

		List<String> communityList = new ArrayList();		
		while (scanner.hasNextLine()) {
			String str = scanner.nextLine();
			str = str.substring(str.indexOf('{') + 1, str.indexOf('}'));
			communityList.add(str);
		}
		
		Collections.sort(communityList, new Comparator<String>() {
			public int compare(String left, String right) {
				return right.length() - left.length();
			}
		});
		
		getDrawDataCommunityShape(callDAG, communityList);
		getDrawDataCommunitySpread(callDAG, communityList);
	}

	
	public static void getCallDAGforWalktrap(CallDAG callDAG, String versionNum) throws Exception {
		PrintWriter pw = new PrintWriter(new File("module_graphs//module-callDAG-" + versionNum + ".txt"));
		for (String s: callDAG.functions) {
			if (callDAG.callTo.containsKey(s)) {
				for (String r: callDAG.callTo.get(s)) {
					pw.println(callDAG.functionID.get(s) + " " + callDAG.functionID.get(r));
				}
			}
		}
		pw.close();
	}
}
