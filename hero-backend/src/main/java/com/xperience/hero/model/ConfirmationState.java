package com.xperience.hero.model;

/**
 * Derived confirmation state (DESIGN.md Step 10). Only meaningful when response = YES;
 * NONE otherwise. Written by the system, never directly by the invitee.
 */
public enum ConfirmationState {
	NONE,
	CONFIRMED,
	WAITLISTED
}
