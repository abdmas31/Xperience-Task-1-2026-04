package com.xperience.hero.model;

/** What clients actually need to know: SCHEDULED status collapses to LOCKED once startTime passes (DESIGN.md INV-2). */
public enum EffectiveStatus {
	OPEN,
	CLOSED,
	CANCELLED,
	LOCKED
}
