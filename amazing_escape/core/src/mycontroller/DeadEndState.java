package mycontroller;
/**
 * Represents the states the car can be related to dead ends
 */
public enum DeadEndState {
	NONE,
	UTURN,
	THREEPOINTTURN, // Java doesn't allow enums to start with a number
	REVERSE
}
