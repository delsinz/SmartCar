package mycontroller;

import java.util.LinkedList;

import tiles.GrassTrap;
import tiles.LavaTrap;
import tiles.MudTrap;
import utilities.Coordinate;
import world.WorldSpatial.Direction;

/** Represents the path that the car will take in terms of cell coordinates */
public class Path {
	/** future path of car given in coordinates */
	public LinkedList<Coordinate> latestPath;
	public TrapTraversalStrategy currentStrategy;

	/* current location of car */
	public Coordinate currentLocation;

	
	public Path(){
		this.latestPath = new LinkedList<Coordinate>();
	}
	
	/**
	 * Gets a view of the current path
	 */
	public LinkedList<Coordinate> getRoute() {
		return latestPath;
	}
	
	/**
	 * Was meant to have functionality but was superceded by other functions
	 */
	public void checkRoute() {

	}

	/**
	 * Update route based on current information known by the car
	 * 
	 * @param m
	 * @param currentLocation
	 * @param controller
	 */
	public void updateRoute(MyMap m, Coordinate currentLocation, MyAIController controller) {
		/* Update current location of the car */
		this.currentLocation = currentLocation;

		/*
		 * If car has reached the next destination node of the path, remove it
		 */
		if (!latestPath.isEmpty() && currentLocation == latestPath.getFirst()) {

			latestPath.removeFirst();
		}
		/*
		 * Once path has been exhausted, generate new path depending on state of
		 * car
		 */
		if (latestPath.isEmpty()) {
			if (controller.currentState == State.WALLFOLLOW) {
				/* If car is following the wall */
				int wallSensitivity = controller.wallSensitivity;

				/* If car is following wall, predict the resulting path */
				if (!controller.isFollowingWall) {

					int x = currentLocation.x;
					int y = currentLocation.y;
					Coordinate newDestination = new Coordinate(x, y);
					boolean encounteredWall = false;
					// first destination is going up until encountering wall
					while (m.getCell(newDestination) != null) {
						
						if (m.getCell(newDestination).getName() != "Wall") {
							newDestination = new Coordinate(newDestination.x, newDestination.y + 1);
						} else {
							encounteredWall = true;
							break;
						}
					}
					/* taking into account wall sensitivity */
					newDestination = new Coordinate(newDestination.x, newDestination.y - wallSensitivity);

					/* add to path if new destination is valid */
					if (newDestination.y > currentLocation.y + wallSensitivity) {
						latestPath.add(newDestination);
					}

					/*
					 * if wall was encountered during calculation of 1st path,
					 * predict path along wall
					 */
					Coordinate newDestination2;
					boolean encounteredSecondWall = false;
					if (encounteredWall) {
						if (latestPath.isEmpty()) {
							/* if car is already along wall */
							newDestination2 = currentLocation;
						} else {
							newDestination2 = newDestination;
						}
						/*
						 * iterate over cells to right of current cell until
						 * wall or unknown cell is encountered
						 */
						while (m.getCell(newDestination2) != null) {
							if (m.getCell(newDestination2).getName() != "Wall") {

								newDestination2 = new Coordinate(newDestination2.x+1, newDestination2.y);

							} else {
								encounteredSecondWall = true;
								break;
							}
						}
						/* taking into account well sensitivity */
						newDestination2 = new Coordinate(newDestination.x - wallSensitivity, newDestination.y);
						/* add to path if new destination is valid */
						if (newDestination.x > currentLocation.x + wallSensitivity) {
							latestPath.add(newDestination2);
						}
					}

				} else {
					// car IS followign wall

					/* extract orientatino of car */
					Direction orientation = controller.getOrientation();
					Coordinate newDestination = currentLocation;
					boolean encounteredWall = false;

					/*
					 * According to which direction car is facing, iterate over
					 * the cells in front until a wall or unknown cell is
					 * reached.
					 * 
					 * The resulting cell is then checked to see if valid, and
					 * if so, is added to path
					 */
					if (orientation == Direction.EAST) {
						while (m.getCell(newDestination) != null) {
							if (m.getCell(newDestination).getName() != "Wall") {
								newDestination = new Coordinate(newDestination.x + 1, newDestination.y);
							} else {
								encounteredWall = true;
								break;
							}
						}
						newDestination = new Coordinate(newDestination.x - wallSensitivity, newDestination.y);
						if (newDestination.x > currentLocation.x + wallSensitivity) {
							latestPath.add(newDestination);
						}
					} else if (orientation == Direction.NORTH) {

						while (m.getCell(newDestination) != null) {
							if (m.getCell(newDestination).getName() != "Wall") {
								newDestination = new Coordinate(newDestination.x, newDestination.y + 1);
							} else {
								encounteredWall = true;
								break;
							}
						}
						newDestination = new Coordinate(newDestination.x, newDestination.y - wallSensitivity);
						if (newDestination.y > currentLocation.y + wallSensitivity) {
							latestPath.add(newDestination);
						}
					} else if (orientation == Direction.WEST) {

						while (m.getCell(newDestination) != null) {
							if (m.getCell(newDestination).getName() != "Wall") {
								newDestination = new Coordinate(newDestination.x - 1, newDestination.y);
							} else {
								encounteredWall = true;
								break;
							}
						}
						newDestination = new Coordinate(newDestination.x + wallSensitivity, newDestination.y);
						if (newDestination.x < currentLocation.x - wallSensitivity) {
							latestPath.add(newDestination);
						}
					} else if (orientation == Direction.SOUTH) {

						while (m.getCell(newDestination) != null) {
							if (m.getCell(newDestination).getName() != "Wall") {
								newDestination = new Coordinate(newDestination.x, newDestination.y - 1);
							} else {
								encounteredWall = true;
								break;
							}
						}
						newDestination = new Coordinate(newDestination.x, newDestination.y + wallSensitivity);
						if (newDestination.x < currentLocation.y - wallSensitivity) {
							latestPath.add(newDestination);
						}
					}
				}

			} else if (controller.currentState == State.ROUTEFOLLOW) {
				// TODO
			}

			/*
			 * if traps are detected along next path, AI is set to follow route
			 * in order to handle traps appropriately
			 */
			Coordinate destination = this.getNextDestination();
			if (destination != null) {
				boolean existingTraps = checkTraps(m, currentLocation, destination);
				controller.setCurrentState(State.ROUTEFOLLOW);
			}

		}
	}
	
	/**
	 * Gets the next coordinate in the path
	 * @return next Coordinate in the path
	 */
	public Coordinate getNextDestination() {
		return this.latestPath.isEmpty() ? null : this.latestPath.getFirst();
	}
	
	/**
	 * Checks if a trap exists between two points
	 * @param m know view of map
	 * @param destination point 1
	 * @param destination2 point 2
	 * @return true if traps exist
	 */
	public boolean checkTraps(MyMap m, Coordinate destination, Coordinate destination2) {
		boolean grass = false;
		boolean lava = false;
		boolean mud = false;
		boolean traps = false;
		/* check every cell between the two locations for traps */
		if (destination.x == destination2.x) {
			int x = destination.x;
			int y;
			if (destination.y < destination2.y) {
				for (y = destination.y; y < destination2.y; y++) {
					Coordinate coordinate = new Coordinate(x, y);
					if (m.getCell(coordinate) instanceof GrassTrap) {
						grass = true;
					} else if (m.getCell(coordinate) instanceof LavaTrap) {
						lava = true;
					} else if (m.getCell(coordinate) instanceof MudTrap) {
						mud = true;
					}
				}
			} else {
				for (y = destination2.y; y < destination.y; y++) {
					Coordinate coordinate = new Coordinate(x, y);
					if (m.getCell(coordinate) instanceof GrassTrap) {
						grass = true;
					} else if (m.getCell(coordinate) instanceof LavaTrap) {
						lava = true;
					} else if (m.getCell(coordinate) instanceof MudTrap) {
						mud = true;
					}
				}
			}
		} else if (destination.y == destination2.y) {
			int x;
			int y = destination.y;
			if (destination.x < destination2.x) {
				for (x = destination.x; x < destination2.x; x++) {
					Coordinate coordinate = new Coordinate(x, y);
					if (m.getCell(coordinate) instanceof GrassTrap) {
						grass = true;
					} else if (m.getCell(coordinate) instanceof LavaTrap) {
						lava = true;
					} else if (m.getCell(coordinate) instanceof MudTrap) {
						mud = true;
					}
				}
			} else {
				for (x = destination2.x; x < destination.x; x++) {
					Coordinate coordinate = new Coordinate(x, y);
					if (m.getCell(coordinate) instanceof GrassTrap) {
						grass = true;
					} else if (m.getCell(coordinate) instanceof LavaTrap) {
						lava = true;
					} else if (m.getCell(coordinate) instanceof MudTrap) {
						mud = true;
					}
				}
			}

		}

		/*
		 * if only one type of trap, use corresponding strat, but if multiple
		 * traps exist, use default trap handling strategy
		 */
		if (grass || lava || mud) {
			TrapTraversalStrategy strat;
			traps = true;
			if (grass && !lava && !mud) {
				strat = TrapTraversalStrategyFactory.getTrapStrategy("grass");
			} else if (!grass && lava && !mud) {
				strat = TrapTraversalStrategyFactory.getTrapStrategy("lava");
			} else if (!grass && !lava && mud) {
				strat = TrapTraversalStrategyFactory.getTrapStrategy("mud");
			} else {
				strat = TrapTraversalStrategyFactory.getTrapStrategy("default");
			}

			// just for the sake of it working, just go with default trap
			strat = TrapTraversalStrategyFactory.getTrapStrategy("default");

			getSafestPath(m, strat);
		}
		return traps;
	}
	
	/**
	 * Updates latest path based on the given traversal strategy
	 * @param m known map
	 * @param t selected traversal strategy
	 */
	public void getSafestPath(MyMap m, TrapTraversalStrategy t) {
		if(t == null){
			System.out.println("t is null");
		}
		LinkedList<Coordinate> newPath = t.getPath(m, this.currentLocation, this.getNextDestination());
		LinkedList<Coordinate> intermediatePath;
		intermediatePath = this.latestPath;
		intermediatePath.removeFirst();
		newPath.addAll(intermediatePath);
		this.latestPath = newPath;

	}
	
	/**
	 * Checks if currently in dead end
	 * @param view known map
	 * @param currentLocation current location of vehicle
	 * @return true if in a dead end state, i.e. at least walled in 3 directions
	 */
	public boolean checkDeadEnd(MyMap view, Coordinate currentLocation) {
		int x = currentLocation.x;
		int y = currentLocation.y;
		int range = 3;
		int sidesWalled = 0;
		int targetX, targetY;
		// Check up side
		for (int i = 0; i < range ; i++) {
			targetX = x;
			targetY = Math.max(y - range, 0);
			Coordinate targetCoord = new Coordinate(targetX + "," + targetY);
			String tileName = view.getCell(targetCoord).getName();
			if (tileName.equals("Wall") || tileName.equals("Trap")) {
				sidesWalled ++;
				break;
			}
		}
		// Check down side
		for (int i = 0; i < range ; i++) {
			targetX = x;
			targetY = y + range;
			Coordinate targetCoord = new Coordinate(targetX + "," + targetY);
			String tileName = view.getCell(targetCoord).getName();
			if (tileName.equals("Wall") || tileName.equals("Trap")) {
				sidesWalled ++;
				break;
			}
		}
		// Check left side
		for (int i = 0; i < range ; i++) {
			targetX = x - range;
			targetY = y;
			Coordinate targetCoord = new Coordinate(targetX + "," + targetY);
			String tileName = view.getCell(targetCoord).getName();
			if (tileName.equals("Wall") || tileName.equals("Trap")) {
				sidesWalled ++;
				break;
			}
		}
		// Check right side
		for (int i = 0; i < range ; i++) {
			targetX = x + range;
			targetY = y;
			Coordinate targetCoord = new Coordinate(targetX + "," + targetY);
			String tileName = view.getCell(targetCoord).getName();
			if (tileName.equals("Wall") || tileName.equals("Trap")) {
				sidesWalled ++;
				break;
			}
		}

		if (sidesWalled >= 3) {
			return true;
		} else {
			return false;
		}
	}
}
