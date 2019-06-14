package mycontroller;

import tiles.TrapTile;

/*8 Factory class for generating the trap traversal strategies */
public class TrapTraversalStrategyFactory {
	public static TrapTraversalStrategy getTrapStrategy(String trap) {
		switch(trap){
		default:
			return new DefaultStrategy();
		}
		
	}
}
