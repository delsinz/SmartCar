package mycontroller;

import java.util.LinkedList;

import tiles.UtilityTile;
import utilities.Coordinate;

/**
 * Default strategy for handling traps, including when multiple traps exist
 */
public class DefaultStrategy implements TrapTraversalStrategy {

	@Override
	public LinkedList<Coordinate> getPath(MyMap m, Coordinate curr, Coordinate dest) {

		LinkedList<Coordinate> route = new LinkedList<Coordinate>();
		route.add(dest);
//		int rad = 1;
//		boolean complete = false;
//		
//		
//		// expand in concentric squares looking for thinnest path
//		while(!complete){
//			// define current enclosed space
//			for(int dir = 0; dir < 4; dir++){
//				// look down, right, up, left 
//			}
//		}
//		
//		// return list of coords from curr to spot just after trap
//		
		return route;
	}
}
