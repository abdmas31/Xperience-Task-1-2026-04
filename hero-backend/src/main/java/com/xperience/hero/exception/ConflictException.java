package com.xperience.hero.exception;

/** Thrown when a write is rejected because of the event/RSVP state machine (locked, closed, cancelled, duplicate invite). */
public class ConflictException extends RuntimeException {
	public ConflictException(String message) {
		super(message);
	}
}
