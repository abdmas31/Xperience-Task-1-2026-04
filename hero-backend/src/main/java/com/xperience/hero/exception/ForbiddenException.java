package com.xperience.hero.exception;

/** Thrown when the caller is not the host of the event they're trying to act on (INV-6, DESIGN.md Step 11). */
public class ForbiddenException extends RuntimeException {
	public ForbiddenException(String message) {
		super(message);
	}
}
