package mycontroller;

import java.util.HashMap;
import java.util.LinkedList;

import com.badlogic.gdx.math.Vector2;

import controller.CarController;
import tiles.MapTile;
import utilities.Coordinate;
import utilities.PeekTuple;
import world.Car;
import world.WorldSpatial;
import world.WorldSpatial.Direction;

public class MyAIController extends CarController {

	public State currentState;
	public DeadEndState deadEndState;
	public Cardinal currentCardinal;
	public LinkedList<Coordinate> latestPath;
	public MyMap knownMap;
	public Path route;

	// How many minimum units the wall is away from the player.
	public int wallSensitivity = 2;

	public boolean isFollowingWall = false; // This is initialized when the car
											// sticks to a wall.
	private WorldSpatial.RelativeDirection lastTurnDirection = null;
	public boolean isTurningLeft = false;
	public boolean isTurningRight = false;
	private WorldSpatial.Direction previousState = null; // Keeps track of the
															// previous state

	// Offset used to differentiate between 0 and 360 degrees
	private int EAST_THRESHOLD = 3;

	// Car speed to move at
	private final float CAR_SPEED = 3;
	private final float TURN_SPEED = 3;
	private final float REVERSE_SPEED = 2;

	private boolean goingForward = true;

	/* Used for dead end calculations */
	private Direction intermediateDirection;
	private Direction backDirection;

	private Coordinate currentLocation;

	public MyAIController(Car car) { // added a constructor
		super(car);
		this.currentState = State.WALLFOLLOW; // Just to start, follow wall and explore possible area
		this.deadEndState = DeadEndState.NONE; // Just to start, assume there's no deadends
		this.knownMap = new MyMap();
		this.route = new Path();
	}

	@Override
	public void update(float delta) {

		HashMap<Coordinate, MapTile> currentView = getView();
		this.currentLocation = new Coordinate(this.getPosition());
		
		

		/* Update map based on current view of car */
		boolean mapChanged = knownMap.updateMap(currentView);

		/* Update route */
		if (mapChanged) {
			route.updateRoute(knownMap, currentLocation, this);
		}

		/* acquire information about car */
		float velocity = this.getVelocity();
		float angle = this.getAngle();
		Direction orientation = this.getOrientation();

		/*
		 * if car's dead end state is set to none, car operates under normal
		 * behaviour
		 */
		if (this.deadEndState == DeadEndState.NONE) {
			boolean deadEnd = route.checkDeadEnd(knownMap, currentLocation);

			/*
			 * if car is identified as being in a dead end (AKA needs to turn
			 * around)
			 */
			if (deadEnd) {
				Vector2 velocity_raw = this.getRawVelocity();
				/* used to predict whether car can make 180 degree turn */
				PeekTuple peekHardRight = this.peek(velocity_raw, 180 - angle, WorldSpatial.RelativeDirection.RIGHT,
						delta);
				/* used to predict whether car can make 90 degree turn */
				PeekTuple peekSoftRight = this.peek(velocity_raw, 90 + angle, WorldSpatial.RelativeDirection.RIGHT,
						delta);
				/* used to track direction car is travelling in */
				backDirection = this.getBackCardinalDirection();

				/* if the car can make a U-Turn */
				if (peekHardRight.getReachable()) {
					this.setDeadEndCarState(DeadEndState.UTURN);
				} else if (peekSoftRight.getReachable()) {
					/*
					 * otherwise, if the car can make a 90 degree turn, do a 3
					 * point turn intsead
					 */
					this.setDeadEndCarState(DeadEndState.THREEPOINTTURN);
					this.intermediateDirection = this.getRightCardinalDirection();
				} else {
					/* if no room for either, simply reverse out */
					this.setDeadEndCarState(DeadEndState.REVERSE);
				}
			} else {
				/*
				 * if no dead end, follow default behaviour depending on car
				 * state
				 */
				if (this.currentState == State.ROUTEFOLLOW) {
//					followPath(delta);
					// time ran out
					followWall(delta, currentView);
					this.setCurrentState(State.WALLFOLLOW);
				} else if (this.currentState == State.WALLFOLLOW) {
					followWall(delta, currentView);
				}
			}
		} else if (this.deadEndState == DeadEndState.REVERSE) {
			/* if car is reversing */
			if (velocity < REVERSE_SPEED) {
				this.applyReverseAcceleration();
			}
		} else if (this.deadEndState == DeadEndState.THREEPOINTTURN) {
			/* if car is completing 3 point turn */
			if (this.goingForward) {
				/* 1st stage of 3 point turn, turning right 90 degrees */
				this.applyRightTurn(orientation, delta);
				lastTurnDirection = WorldSpatial.RelativeDirection.RIGHT;

				/* once car has turned 90 degrees, brake and start reversing */
				if (orientation == intermediateDirection) {
					this.applyBrake();
					this.goingForward = false;
				} else {
					if (velocity < TURN_SPEED) {
						this.applyForwardAcceleration();
					}
				}
			} else {
				/* 2nd stage, reversing to the left */
				this.applyLeftTurn(orientation, delta);
				lastTurnDirection = WorldSpatial.RelativeDirection.LEFT;

				/*
				 * once car is oriented correct way, brake and start moving
				 * forward
				 */
				if (orientation == backDirection) {
					this.applyBrake();
					this.goingForward = true;
					this.deadEndState = DeadEndState.NONE;
				} else {
					if (velocity < REVERSE_SPEED) {
						this.applyReverseAcceleration();
					}
				}
			}
		} else if (this.deadEndState == DeadEndState.UTURN) {
			/* apply right turn until car is facing correct way */
			this.applyRightTurn(orientation, delta);
			lastTurnDirection = WorldSpatial.RelativeDirection.RIGHT;

			if (orientation == backDirection) {
				this.deadEndState = DeadEndState.NONE;
			}
			if (velocity < TURN_SPEED) {
				this.applyForwardAcceleration();
			}
		}
	}

	/** Follows path defined in route */
	private void followPath(float delta) {
		// follow the path
		Coordinate nextDest = route.getNextDestination();
		Direction orientation = this.getOrientation();
		// nothing left to follow in path, revert to following wall
		if(nextDest != null){
			// follow path
			switch(orientation){
			case EAST:
				// next dest is in front
				if(nextDest.y == currentLocation.y && nextDest.x > currentLocation.x){
					this.applyForwardAcceleration();
				}
				// next dest is behind
				else if(nextDest.y == currentLocation.y && nextDest.x < currentLocation.x){
					
					this.applyRightTurn(orientation, delta);
					lastTurnDirection = WorldSpatial.RelativeDirection.RIGHT;
					if (this.getVelocity() < TURN_SPEED) {
						this.applyForwardAcceleration();
					}
				}
				// next dest is left
				else if(nextDest.x == currentLocation.x && nextDest.y > currentLocation.y){
					this.applyLeftTurn(orientation, delta);
					lastTurnDirection = WorldSpatial.RelativeDirection.LEFT;
					this.applyForwardAcceleration();
				}
				// next dest is right
				else if(nextDest.x == currentLocation.x && nextDest.y < currentLocation.y){
					this.applyRightTurn(orientation, delta);
					lastTurnDirection = WorldSpatial.RelativeDirection.RIGHT;
					this.applyForwardAcceleration();
				}
				break;
			case NORTH:
				// next dest is in front
				if(nextDest.x == currentLocation.x && nextDest.y > currentLocation.y){
					this.applyForwardAcceleration();
				}
				// next dest is behind
				else if(nextDest.x == currentLocation.x && nextDest.y < currentLocation.y){
					this.applyRightTurn(orientation, delta);
					lastTurnDirection = WorldSpatial.RelativeDirection.RIGHT;
					if (this.getVelocity() < TURN_SPEED) {
						this.applyForwardAcceleration();
					}
				}
				// next dest is left
				else if(nextDest.y == currentLocation.y && nextDest.x > currentLocation.x){
					this.applyLeftTurn(orientation, delta);
					lastTurnDirection = WorldSpatial.RelativeDirection.LEFT;
					this.applyForwardAcceleration();
				}
				// next dest is right
				else if(nextDest.y == currentLocation.y && nextDest.x < currentLocation.x){
					this.applyRightTurn(orientation, delta);
					lastTurnDirection = WorldSpatial.RelativeDirection.RIGHT;
					this.applyForwardAcceleration();
				}
				break;
			case WEST:
				// next dest is in front
				if(nextDest.y == currentLocation.y && nextDest.x < currentLocation.x){
					this.applyForwardAcceleration();
				}
				// next dest is behind
				else if(nextDest.y == currentLocation.y && nextDest.x > currentLocation.x){
					this.applyRightTurn(orientation, delta);
					lastTurnDirection = WorldSpatial.RelativeDirection.RIGHT;
					if (this.getVelocity() < TURN_SPEED) {
						this.applyForwardAcceleration();
					}
				}
				// next dest is left
				else if(nextDest.x == currentLocation.x && nextDest.y < currentLocation.y){
					this.applyLeftTurn(orientation, delta);
					lastTurnDirection = WorldSpatial.RelativeDirection.LEFT;
					this.applyForwardAcceleration();
				}
				// next dest is right
				else if(nextDest.x == currentLocation.x && nextDest.y > currentLocation.y){
					this.applyRightTurn(orientation, delta);
					lastTurnDirection = WorldSpatial.RelativeDirection.RIGHT;
					this.applyForwardAcceleration();
				}
				break;
			case SOUTH:
				// next dest is in front
				if(nextDest.x == currentLocation.x && nextDest.y < currentLocation.y){
					this.applyForwardAcceleration();
				}
				// next dest is behind
				else if(nextDest.x == currentLocation.x && nextDest.y > currentLocation.y){
					
					this.applyRightTurn(orientation, delta);
					lastTurnDirection = WorldSpatial.RelativeDirection.RIGHT;
					if (this.getVelocity() < TURN_SPEED) {
						this.applyForwardAcceleration();
					}
				}
				// next dest is left
				else if(nextDest.y == currentLocation.y && nextDest.x < currentLocation.x){
					this.applyLeftTurn(orientation, delta);
					lastTurnDirection = WorldSpatial.RelativeDirection.LEFT;
					this.applyForwardAcceleration();
				}
				// next dest is right
				else if(nextDest.y == currentLocation.y && nextDest.x > currentLocation.x){
					this.applyRightTurn(orientation, delta);
					lastTurnDirection = WorldSpatial.RelativeDirection.RIGHT;
					this.applyForwardAcceleration();
				}
				break;
			default:
				this.applyForwardAcceleration();
				break;
			}
		}
	}

	/** Follows wall of maze */
	private void followWall(float delta, HashMap<Coordinate, MapTile> currentView) {

		checkStateChange();
		// If you are not following a wall initially, find a wall to stick to!
		if (!isFollowingWall) {
			if (getVelocity() < CAR_SPEED) {
				applyForwardAcceleration();
			}
			// Turn towards the north
			if (!getOrientation().equals(WorldSpatial.Direction.NORTH)) {
				lastTurnDirection = WorldSpatial.RelativeDirection.LEFT;
				applyLeftTurn(getOrientation(), delta);
			}
			if (checkNorth(currentView)) {
				// Turn right until we go back to east!
				if (!getOrientation().equals(WorldSpatial.Direction.EAST)) {
					lastTurnDirection = WorldSpatial.RelativeDirection.RIGHT;
					applyRightTurn(getOrientation(), delta);
				} else {
					isFollowingWall = true;
				}
			}
		}
		// Once the car is already stuck to a wall, apply the following logic
		else {
			// Readjust the car if it is misaligned.
			readjust(lastTurnDirection, delta);

			if (isTurningRight) {
				applyRightTurn(getOrientation(), delta);
			} else if (isTurningLeft) {
				// Apply the left turn if you are not currently near a wall.
				if (!checkFollowingWall(getOrientation(), currentView)) {
					applyLeftTurn(getOrientation(), delta);
				} else {
					isTurningLeft = false;
				}
			}
			// Try to determine whether or not the car is next to a wall.
			else if (checkFollowingWall(getOrientation(), currentView)) {
				// Maintain some velocity
				if (getVelocity() < CAR_SPEED) {
					applyForwardAcceleration();
				}
				// If there is wall ahead, turn right!
				if (checkWallAhead(getOrientation(), currentView)) {
					lastTurnDirection = WorldSpatial.RelativeDirection.RIGHT;
					isTurningRight = true;

				}

			}
			// This indicates that I can do a left turn if I am not turning
			// right
			else {
				lastTurnDirection = WorldSpatial.RelativeDirection.LEFT;
				isTurningLeft = true;
			}
		}
	}

	/**
	 * Checks whether the car's state has changed or not, stops turning if it
	 * already has.
	 */
	private void checkStateChange() {
		if (previousState == null) {
			previousState = getOrientation();
		} else {
			if (previousState != getOrientation()) {
				if (isTurningLeft) {
					isTurningLeft = false;
				}
				if (isTurningRight) {
					isTurningRight = false;
				}
				previousState = getOrientation();
			}
		}
	}

	/**
	 * Readjust the car to the orientation we are in.
	 * 
	 * @param lastTurnDirection
	 * @param delta
	 */
	private void readjust(WorldSpatial.RelativeDirection lastTurnDirection, float delta) {
		if (lastTurnDirection != null) {
			if (!isTurningRight && lastTurnDirection.equals(WorldSpatial.RelativeDirection.RIGHT)) {
				adjustRight(getOrientation(), delta);
			} else if (!isTurningLeft && lastTurnDirection.equals(WorldSpatial.RelativeDirection.LEFT)) {
				adjustLeft(getOrientation(), delta);
			}
		}

	}

	/**
	 * Try to orient myself to a degree that I was supposed to be at if I am
	 * misaligned.
	 */
	private void adjustLeft(WorldSpatial.Direction orientation, float delta) {

		switch (orientation) {
		case EAST:
			if (getAngle() > WorldSpatial.EAST_DEGREE_MIN + EAST_THRESHOLD) {
				turnRight(delta);
			}
			break;
		case NORTH:
			if (getAngle() > WorldSpatial.NORTH_DEGREE) {
				turnRight(delta);
			}
			break;
		case SOUTH:
			if (getAngle() > WorldSpatial.SOUTH_DEGREE) {
				turnRight(delta);
			}
			break;
		case WEST:
			if (getAngle() > WorldSpatial.WEST_DEGREE) {
				turnRight(delta);
			}
			break;

		default:
			break;
		}

	}

	private void adjustRight(WorldSpatial.Direction orientation, float delta) {
		switch (orientation) {
		case EAST:
			if (getAngle() > WorldSpatial.SOUTH_DEGREE && getAngle() < WorldSpatial.EAST_DEGREE_MAX) {
				turnLeft(delta);
			}
			break;
		case NORTH:
			if (getAngle() < WorldSpatial.NORTH_DEGREE) {
				turnLeft(delta);
			}
			break;
		case SOUTH:
			if (getAngle() < WorldSpatial.SOUTH_DEGREE) {
				turnLeft(delta);
			}
			break;
		case WEST:
			if (getAngle() < WorldSpatial.WEST_DEGREE) {
				turnLeft(delta);
			}
			break;

		default:
			break;
		}

	}

	/**
	 * Turn the car clock wise (think of a compass going clock-wise)
	 */
	private void applyRightTurn(WorldSpatial.Direction orientation, float delta) {
		switch (orientation) {
		case EAST:
			if (!getOrientation().equals(WorldSpatial.Direction.SOUTH)) {
				turnRight(delta);
			}
			break;
		case NORTH:
			if (!getOrientation().equals(WorldSpatial.Direction.EAST)) {
				turnRight(delta);
			}
			break;
		case SOUTH:
			if (!getOrientation().equals(WorldSpatial.Direction.WEST)) {
				turnRight(delta);
			}
			break;
		case WEST:
			if (!getOrientation().equals(WorldSpatial.Direction.NORTH)) {
				turnRight(delta);
			}
			break;
		default:
			break;

		}

	}

	/**
	 * Turn the car counter clock wise (think of a compass going counter
	 * clock-wise)
	 */
	private void applyLeftTurn(WorldSpatial.Direction orientation, float delta) {
		switch (orientation) {
		case EAST:
			if (!getOrientation().equals(WorldSpatial.Direction.NORTH)) {
				turnLeft(delta);
			}
			break;
		case NORTH:
			if (!getOrientation().equals(WorldSpatial.Direction.WEST)) {
				turnLeft(delta);
			}
			break;
		case SOUTH:
			if (!getOrientation().equals(WorldSpatial.Direction.EAST)) {
				turnLeft(delta);
			}
			break;
		case WEST:
			if (!getOrientation().equals(WorldSpatial.Direction.SOUTH)) {
				turnLeft(delta);
			}
			break;
		default:
			break;

		}

	}

	/**
	 * Check if the wall is on your left hand side given your orientation
	 * 
	 * @param orientation
	 * @param currentView
	 * @return
	 */
	private boolean checkFollowingWall(WorldSpatial.Direction orientation, HashMap<Coordinate, MapTile> currentView) {

		switch (orientation) {
		case EAST:
			return checkNorth(currentView);
		case NORTH:
			return checkWest(currentView);
		case SOUTH:
			return checkEast(currentView);
		case WEST:
			return checkSouth(currentView);
		default:
			return false;
		}

	}

	/**
	 * Check if you have a wall in front of you!
	 * 
	 * @param orientation
	 *            the orientation we are in based on WorldSpatial
	 * @param currentView
	 *            what the car can currently see
	 * @return
	 */
	private boolean checkWallAhead(WorldSpatial.Direction orientation, HashMap<Coordinate, MapTile> currentView) {
		switch (orientation) {
		case EAST:
			return checkEast(currentView);
		case NORTH:
			return checkNorth(currentView);
		case SOUTH:
			return checkSouth(currentView);
		case WEST:
			return checkWest(currentView);
		default:
			return false;

		}
	}

	/**
	 * Method below just iterates through the list and check in the correct
	 * coordinates. i.e. Given your current position is 10,10 checkEast will
	 * check up to wallSensitivity amount of tiles to the right. checkWest will
	 * check up to wallSensitivity amount of tiles to the left. checkNorth will
	 * check up to wallSensitivity amount of tiles to the top. checkSouth will
	 * check up to wallSensitivity amount of tiles below.
	 */
	public boolean checkEast(HashMap<Coordinate, MapTile> currentView) {
		// Check tiles to my right
		Coordinate currentPosition = new Coordinate(getPosition());
		for (int i = 0; i <= wallSensitivity; i++) {
			MapTile tile = currentView.get(new Coordinate(currentPosition.x + i, currentPosition.y));
			if (tile.getName().equals("Wall")) {
				return true;
			}
		}
		return false;
	}

	public boolean checkWest(HashMap<Coordinate, MapTile> currentView) {
		// Check tiles to my left
		Coordinate currentPosition = new Coordinate(getPosition());
		for (int i = 0; i <= wallSensitivity; i++) {
			MapTile tile = currentView.get(new Coordinate(currentPosition.x - i, currentPosition.y));
			if (tile.getName().equals("Wall")) {
				return true;
			}
		}
		return false;
	}

	public boolean checkNorth(HashMap<Coordinate, MapTile> currentView) {
		// Check tiles to towards the top
		Coordinate currentPosition = new Coordinate(getPosition());
		for (int i = 0; i <= wallSensitivity; i++) {
			MapTile tile = currentView.get(new Coordinate(currentPosition.x, currentPosition.y + i));
			if (tile.getName().equals("Wall")) {
				return true;
			}
		}
		return false;
	}

	public boolean checkSouth(HashMap<Coordinate, MapTile> currentView) {
		// Check tiles towards the bottom
		Coordinate currentPosition = new Coordinate(getPosition());
		for (int i = 0; i <= wallSensitivity; i++) {
			MapTile tile = currentView.get(new Coordinate(currentPosition.x, currentPosition.y - i));
			if (tile.getName().equals("Wall")) {
				return true;
			}
		}
		return false;
	}

	
	public Direction getForwardCardinalDirection() {
		return this.getOrientation();
	}
	public Direction getBackCardinalDirection() {
		switch(this.getOrientation()){
		case EAST:
			return Direction.WEST;
		case NORTH:
			return Direction.SOUTH;
		case SOUTH:
			return Direction.NORTH;
		case WEST:
			return Direction.EAST;
		default:
			return null;
		}
	}
	public Direction getLeftCardinalDirection() {
		switch(this.getOrientation()){
		case EAST:
			return Direction.NORTH;
		case NORTH:
			return Direction.WEST;
		case SOUTH:
			return Direction.EAST;
		case WEST:
			return Direction.SOUTH;
		default:
			return null;
		}
	}
	public Direction getRightCardinalDirection() {
		switch(this.getOrientation()){
		case EAST:
			return Direction.SOUTH;
		case NORTH:
			return Direction.EAST;
		case SOUTH:
			return Direction.WEST;
		case WEST:
			return Direction.NORTH;
		default:
			return null;
		}
	}

	public void setCurrentState(State currentState) {
		this.currentState = currentState;
	}

	public void setDeadEndCarState(DeadEndState deadEndState) {
		this.deadEndState = deadEndState;
	}

	public State getCurrentState() {
		return currentState;
	}

	public DeadEndState getDeadEndCarState() {
		return deadEndState;
	}

	public MyMap getKnownMap() {
		return knownMap;
	}

	public Path getRoute() {
		return route;
	}

}
