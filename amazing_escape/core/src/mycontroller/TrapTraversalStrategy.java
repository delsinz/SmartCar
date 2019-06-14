package mycontroller;

import java.util.LinkedList;

import utilities.Coordinate;

/** Interface for trap traversal strategies */
public interface TrapTraversalStrategy {
	/**
	 * Determine the best path to travel from current coordinate to destination
	 * coordinate
	 * 
	 * @param m
	 * @param curr
	 * @param dest
	 * @return
	 */
	public LinkedList<Coordinate> getPath(MyMap m, Coordinate curr, Coordinate dest);

}
