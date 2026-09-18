package com.xperience.hero.model;

/** Stored event status (DESIGN.md Step 10). LOCKED is not stored here — it's derived from startTime, see Event#effectiveStatus. */
public enum EventStatus {
	SCHEDULED,
	CLOSED,
	CANCELLED
}
