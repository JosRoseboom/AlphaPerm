package alphabetpermutation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DistributedOptimizer implements Runnable {
	
	/**
	 * Best solutions is a set, since there could be multiple equally valued solutions
	 */
	private static Set<Path> globalBest; 
	private static int	globalBestCost;
	private static long starttime;
	/**
	 * The higher this value, the higher the probability of accepting a worse neighbor solution
	 */
	private static double initialControlParam;
	private static int runsWithSameControlParam;
	private static double controlParamUpdateFactor;
	/**
	 * if the last run with the same control parameter less impairments are accepted, this search will be terminated
	 */
	private static double impairmentAcceptanceMinimum;
	private static List<Character> characters;
	
	private double controlParam;
	private List<Path> currentPath;
	private int runsWithCurrentControlParam;
	private int acceptedImpairments;
	
	public static void optimize(List<Character> chars, Map<Character, Map<Character, Set<Integer>>> victimsPerCombination, int nThreads){
		characters 									= 	chars;
		starttime									=	System.currentTimeMillis();
		globalBest									=	Collections.synchronizedSet(new HashSet<Path>());
		initialControlParam							=	40;
		runsWithSameControlParam					=	1000;
		controlParamUpdateFactor					=	0.9;
		impairmentAcceptanceMinimum					=	0.01;
		Path.init(victimsPerCombination);

		for(int i = 0; i < nThreads; i++)
			new Thread(new DistributedOptimizer(), "Thread " + i).start();
	}
	
	private DistributedOptimizer(){
		this.controlParam					=	initialControlParam;
		this.currentPath					=	new ArrayList<Path>(characters.size());
		List<Character> randomPermutation	=	new ArrayList<Character>(characters);
		Path parentPath						=	new Path(null,null,null);
		runsWithCurrentControlParam			=	0;
		acceptedImpairments					=	0;
		
		Collections.shuffle(randomPermutation);
		
		for(Character charToAppend : randomPermutation){
			parentPath	=	new Path(parentPath, charToAppend, null);
			currentPath.add(parentPath);
		}
		
		synchronized(DistributedOptimizer.class){
			if(globalBest.isEmpty()){
				globalBest.add(parentPath);
				globalBestCost	=	parentPath.getPathCost();
			}
		}
	}
	
	@Override
	public void run() {
		boolean shouldContinue	=	true;
		while(shouldContinue){
			List<Path> neighborSolution;
			int fromIndex		=	(int) (Math.random() * currentPath.size());
			int toIndex			=	(int) (Math.random() * currentPath.size()-1);
			if(toIndex == fromIndex)
				toIndex	= currentPath.size()-1;
			
			double random	=	Math.random();
			if(random < 0.4)
				neighborSolution = fromIndex < toIndex ? swapElements(fromIndex, toIndex) : swapElements(toIndex, fromIndex);
			else if(random < 0.8)
				neighborSolution = fromIndex < toIndex ? moveElementToTheRight(fromIndex, toIndex) : moveElementToTheLeft(toIndex, fromIndex);
			else
				neighborSolution = fromIndex < toIndex ? shuffleSubPath(fromIndex, toIndex) : shuffleSubPath(toIndex, fromIndex);	
				
			if(shouldAccept(neighborSolution))
				currentPath	=	neighborSolution;
			
			runsWithCurrentControlParam++;
			
			if(runsWithCurrentControlParam >= runsWithSameControlParam){
				System.out.println("Share of accepted solutions with control param " + controlParam + " is " + (acceptedImpairments/(double)runsWithSameControlParam));
				runsWithCurrentControlParam	=	0;
				shouldContinue				=	(acceptedImpairments/(double)runsWithSameControlParam) >= impairmentAcceptanceMinimum;
				controlParam 				*= controlParamUpdateFactor;
				acceptedImpairments			=	0;
			}		
		}
		finish();
	}
	
	private void finish(){
		System.out.println((System.currentTimeMillis() - starttime)/60000 + " minutes ran. " + Thread.currentThread().getName() + " has finished a job.");
		synchronized(DistributedOptimizer.class){
			System.out.println("\tCurrent global best solutions block " + globalBestCost + " words. Best solutions are:");
			for(Path path : globalBest)
				System.out.println("\t" + path.getPath().toString());
		}
		
		new DistributedOptimizer().run();
	}

	private List<Path> moveElementToTheLeft(int leftIndex, int rightIndex) {
		List<Path> candidateSolution	=	new ArrayList<Path>(currentPath.size());
		candidateSolution.addAll(currentPath.subList(0, leftIndex));
		
		candidateSolution.add(new Path(currentPath.get(leftIndex).getParent(), currentPath.get(rightIndex).extension, null));
		
		for(int i = leftIndex + 1; i <= rightIndex; i++)
			candidateSolution.add(new Path(candidateSolution.get(i - 1), currentPath.get(i-1).extension, null));
		
		for(int i =	rightIndex + 1; i < currentPath.size(); i++)
			candidateSolution.add(new Path(candidateSolution.get(i - 1), currentPath.get(i).extension, currentPath.get(i).getPositionLosses()));
		
		return candidateSolution;
	}

	private List<Path> moveElementToTheRight(int leftIndex, int rightIndex) {
		List<Path> candidateSolution	=	new ArrayList<Path>(currentPath.size());
		candidateSolution.addAll(currentPath.subList(0, leftIndex));
		
		for(int i = leftIndex; i < rightIndex; i++){
			Path parent	=	i == 0 ? new Path(null,null,null) : candidateSolution.get(i-1);
			candidateSolution.add(new Path(parent, currentPath.get(i + 1).extension, null));
		}
		
		candidateSolution.add(new Path(candidateSolution.get(rightIndex-1), currentPath.get(leftIndex).extension, null));
		
		for(int i =	rightIndex + 1; i < currentPath.size(); i++)
			candidateSolution.add(new Path(candidateSolution.get(i - 1), currentPath.get(i).extension, currentPath.get(i).getPositionLosses()));
		
		return candidateSolution;
	}

	private List<Path> swapElements(int leftIndex, int rightIndex) {
		List<Path> candidateSolution	=	new ArrayList<Path>(currentPath.size());
		candidateSolution.addAll(currentPath.subList(0, leftIndex));
		
		candidateSolution.add(new Path(currentPath.get(leftIndex).getParent(), currentPath.get(rightIndex).extension, null));
		
		for(int i = leftIndex + 1; i < rightIndex; i++)
			candidateSolution.add(new Path(candidateSolution.get(i - 1), currentPath.get(i).extension, null));
		
		candidateSolution.add(new Path(candidateSolution.get(rightIndex-1), currentPath.get(leftIndex).extension, null));
		
		for(int i =	rightIndex + 1; i < currentPath.size(); i++)
			candidateSolution.add(new Path(candidateSolution.get(i - 1), currentPath.get(i).extension, currentPath.get(i).getPositionLosses()));
		
		return candidateSolution;
	}
	
	private List<Path> shuffleSubPath(int leftIndex, int rightIndex){
		List<Path> candidateSolution	=	new ArrayList<Path>(currentPath.size());
		List<Path> subPath				=	new ArrayList<Path>(currentPath).subList(leftIndex, rightIndex + 1);
		Collections.shuffle(subPath);
		
		candidateSolution.addAll(currentPath.subList(0, leftIndex));
		
		for(int i = 0; i < subPath.size(); i++){
			Path parent	=	candidateSolution.isEmpty() ? new Path(null,null,null) : candidateSolution.get(candidateSolution.size()-1);
			candidateSolution.add(new Path(parent, subPath.get(i).extension, null));
		}
		
		for(int i =	rightIndex + 1; i < currentPath.size(); i++)
			candidateSolution.add(new Path(candidateSolution.get(i - 1), currentPath.get(i).extension, currentPath.get(i).getPositionLosses()));
		
		if(candidateSolution.get(candidateSolution.size()-1).getPath().size()!=currentPath.size())
			System.out.println("stop");
		return candidateSolution;
	}	

	private boolean shouldAccept(List<Path> neighborSolution) {
		int currentCost		=	getPathCost(currentPath);
		int candidateCost	=	getPathCost(neighborSolution);
		
		if(candidateCost <= globalBestCost){
			synchronized(DistributedOptimizer.class){
				globalBestCost		=	new ArrayList<Path>(globalBest).get(0).getPathCost();
				Path neighborPath	=	neighborSolution.get(neighborSolution.size()-1);
				if(candidateCost < globalBestCost){
					globalBest.clear();
					globalBestCost	=	candidateCost;
				}
				
				if(globalBest.add(neighborPath.copy()))
					System.out.println((System.currentTimeMillis() - starttime)/60000 + " minutes ran. " + Thread.currentThread().getName() + " found path " + neighborPath.getPath().toString() + " is added to the top paths, blocking " + neighborPath.getPathCost() + " words");
			}
		}
		
		if(candidateCost <= currentCost)
			return true;
		else{
			boolean accept	=	Math.random()< Math.pow(2.0, (currentCost - candidateCost)/controlParam);
			if(accept)
				acceptedImpairments++;
			return accept;
		}
	}
	
	private int getPathCost(List<Path> path){
		return path.get(path.size()-1).getPathCost();
	}
}