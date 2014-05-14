package alphabetpermutation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Path {
	
	public final Character extension;
	private static Map<Character, Map<Character, Set<Integer>>> victimsPerCombination;
	private final Path parent;
	private Set<Integer> totalLossesCache;
	private Set<Integer> positionLossesCache;
	 
	public static void init(Map<Character, Map<Character, Set<Integer>>> victimsPerCombination){
		Path.victimsPerCombination	=	victimsPerCombination;
	}
	
	public Path(Path parent, Character extension, Set<Integer> positionLossesCache){
		this.parent					=	parent;
		this.extension				=	extension;
		if(positionLossesCache != null)
			this.positionLossesCache	=	new HashSet<Integer>(positionLossesCache);
	}
	
	public Path getParent(){
		return parent;
	}

	public int getPathCost(){
		return getLosses().size();
	}
	
	/**
	 * 
	 * @return the losses caused by the relation of this path element with its ancestors
	 */
	public Set<Integer> getPositionLosses(){
		if(positionLossesCache == null){
			positionLossesCache	=	new HashSet<Integer>();
			Path ancestor		=	parent;
			while(ancestor.extension != null){
				positionLossesCache.addAll(victimsPerCombination.get(ancestor.extension).get(extension));
				ancestor	=	ancestor.parent;
			}
		}
		return positionLossesCache;
	}
	
	public List<Character> getPath(){
		List<Character> path	=	new ArrayList<Character>();
		
		if(parent.extension != null)
			path.addAll(parent.getPath());
		path.add(extension);
		
		return path;
	}
	
	public Path copy(){
		if(parent == null)
			return new Path(null,null,null);
		else
			return new Path(parent.copy(), extension, totalLossesCache == null ? null : new HashSet<Integer>(totalLossesCache));
	}
	
	/**
	 * 
	 * @return the losses of the total path up to this path element
	 */
	private Set<Integer> getLosses(){
		if(totalLossesCache == null)
			totalLossesCache	=	getPathLossesByComputation();
		return totalLossesCache;
	}
	
	private final Set<Integer> getPathLossesByComputation(){
		Set<Integer> newLosses	=	new HashSet<Integer>();
		if(parent.extension != null){
			newLosses.addAll(parent.getLosses());
			newLosses.addAll(getPositionLosses());
		}
		
		return newLosses;
	}
	
	/**
	 * hashCode and equals are expensive (because the getPath() call). No problem, since only used if a global best solution is found.
	 */
	
	public int hashCode(){
		return getPath().hashCode();
	}
	
	public boolean equals(Object o){
		return this ==o || o instanceof Path && ((Path) o).getPath().equals(getPath());
	}
}