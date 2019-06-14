package mycontroller;

import java.util.HashMap;

import tiles.MapTile;
import utilities.Coordinate;

/** Represents the known map to the car */
public class MyMap {

	/** map of known tiles */
	public HashMap<Coordinate, MapTile> grid;

	public MyMap(){
		this.grid = new HashMap<Coordinate, MapTile> ();
	}

	/** Update map based on current view of car */
	public boolean updateMap(HashMap<Coordinate, MapTile> cv) {

		boolean mapChange = false;
		for (Coordinate coordinate : cv.keySet()) {
			if (grid.containsKey(coordinate)) {
				continue;
			} else {
				grid.put(coordinate, cv.get(coordinate));
				mapChange = true;
			}
		}

		return mapChange;

	}

	/** Retrieve the map tile at given coordinates */
	public MapTile getCell(Coordinate c) {

		return grid.get(c);

	}
}
