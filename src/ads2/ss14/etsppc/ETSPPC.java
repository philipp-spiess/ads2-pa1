package ads2.ss14.etsppc;

import java.util.*;
import java.util.Map.Entry;

/**
 * Klasse zum Berechnen der Tour mittels Branch-and-Bound.
 * Hier sollen Sie Ihre L&ouml;sung implementieren.
 */
public class ETSPPC extends AbstractETSPPC {
	
	private ETSPPCInstance instance;
	private double L;
	
	public ETSPPC(ETSPPCInstance instance) {
		this.instance = instance;

		// local lower bound without any already visited locations = global lower bound
		this.L = getLocalLowerBound(new HashMap<Integer, Location>(), 0);
	}

	/**
	 * Diese Methode bekommt vom Framework maximal 30 Sekunden Zeit zur
	 * Verf&uuml;gung gestellt um eine g&uuml;ltige Tour
	 * zu finden.
	 */
	@Override
	public void run() {
		Map<Integer, Location> possibleStartLocations = possibleStartLocations();

		for(Entry<Integer, Location> startLocation : possibleStartLocations.entrySet()) {
			// Add startLocation to visited locations
			Map<Integer, Location> visitedLocations = new HashMap<Integer, Location>();
			visitedLocations.put(startLocation.getKey(), startLocation.getValue());
			
			// Add startLocation to visited order
			List<Location> visitedOrder = new LinkedList<Location>();
			visitedOrder.add(startLocation.getValue());
			
			// Start the recursion for the following start node
			findSolution(startLocation, startLocation, visitedLocations, visitedOrder, 0);
		}
	}
	
	private void findSolution(Entry<Integer, Location> startLocation, Entry<Integer, Location> currentLocation, Map<Integer, Location> visitedLocations, List<Location> visitedOrder, double currentBound) {
		double U = this.getBestSolution() == null ? Double.MAX_VALUE : this.getBestSolution().getUpperBound();
		double L = this.L;
		// If we didn't select any path yet, we don't need to calculate a local lower bound
		if(visitedLocations.size() > 1) 
			L = getLocalLowerBound(visitedLocations, currentBound);
		
		// We can end recursion here, because we cannot find a better solution then the current best in this subtree
		if(L >= U)
			return;
		
		// If we already exceed our upper bound, stop
		if(currentBound >= U)
			return;
		
		
		// Loop through all neighbours
		for(Entry<Double, Entry<Integer, Location>> entry : getPossibleNeighbours(currentLocation, visitedLocations).entrySet()) {
			double distance = entry.getKey();
			double bound = currentBound + distance;
			Entry<Integer, Location> neighbour = entry.getValue();
			
			// create a clone to add this neighbour to the visited locations map
			Map<Integer, Location> visitedLocationsClone = new HashMap<Integer, Location>();
			visitedLocationsClone.putAll(visitedLocations);
			visitedLocationsClone.put(neighbour.getKey(), neighbour.getValue());
			
			// create a clone to add this neighbour to the ordered visited locations list
			List<Location> visitedOrderClone = new LinkedList<Location>();
			visitedOrderClone.addAll(visitedOrder);
			visitedOrderClone.add(neighbour.getValue());
			
			// Check if we're finished
			if(visitedLocationsClone.size() >= instance.getAllLocations().size()) {
				// Now we "just" have to go back to the first location
				double way_home = neighbour.getValue().distanceTo(startLocation.getValue());
				this.setSolution(bound + way_home, visitedOrderClone);
			}
			
			// Recursion!
			findSolution(startLocation, neighbour, visitedLocationsClone, visitedOrderClone, bound);
		}
		
		//Main.printDebug(getPossibleNeighbours(currentLocation, visitedLocations));
	}
	
	private SortedMap<Double, Entry<Integer, Location>> getPossibleNeighbours(Entry<Integer, Location> location, Map<Integer, Location> visitedLocations) {
		SortedMap<Double, Entry<Integer, Location>> sorted_neighbours = new TreeMap<Double, Entry<Integer, Location>>();
		loop:
		for(Entry<Integer, Location> neighbour : instance.getAllLocations().entrySet()) {
			// A location is not the neighbour of itself
			if(neighbour.getKey().equals(location.getKey()))
				continue loop;
			// Skip locations that are already visited
			if(visitedLocations.containsKey(neighbour.getKey()))
				continue loop;
			// Skip if doesn't yet meet precedence constraints
			for(PrecedenceConstraint constraint : instance.getConstraints()) {
				// only if there is a constraint on this node
				if(neighbour.getKey() == constraint.getSecond()) {
					if(!visitedLocations.containsKey(constraint.getFirst())) {
						continue loop;
					}
				}
			}
			
			sorted_neighbours.put(location.getValue().distanceTo(neighbour.getValue()), neighbour);
		}
		return sorted_neighbours;
	}
	
	/**
	 * Diese Methode berechnet eine untere schranke, in dem es von jedem Konten den kleisnten Weg auswählt.
	 * Diese Schranke ist bei zusätzlichen Bedingungen nur local gültig.
	 */
	private double getLocalLowerBound(Map<Integer, Location> visitedLocations, double lower_bound) {
		for(Entry<Integer, Location> location : instance.getAllLocations().entrySet()) {

			// Loop through all locations who are not already added
			if(!visitedLocations.containsKey(location.getKey())) {
				double min_distance = Double.MAX_VALUE;
				 
				// Find the nearest neigbour and add the distance
				for(Entry<Integer, Location> neighbour : instance.getAllLocations().entrySet()) {
					if(neighbour.getKey().equals(location.getKey()))
						continue;
					
					double distance = location.getValue().distanceTo(neighbour.getValue());
					if(distance < min_distance) {
						min_distance = distance;
					}
				}
				lower_bound += min_distance;
			}
		}
		return lower_bound;
	}
	
	/**
	 * Diese Methode sucht gültige Startknoten. Dies sind Knoten, welche in keiner Constraint an zweiter stelle vorkommen.
	 */
	private Map<Integer, Location> possibleStartLocations() {
		Map<Integer, Location> possibleStartLocations = new HashMap<Integer, Location>();
		possibleStartLocations.putAll(instance.getAllLocations()); 
		for(PrecedenceConstraint constraint : instance.getConstraints()) {
			possibleStartLocations.remove(constraint.getSecond());
		}
		return possibleStartLocations;
	}

}
