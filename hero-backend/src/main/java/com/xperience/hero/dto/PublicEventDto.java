package com.xperience.hero.dto;

import java.time.Instant;

import com.xperience.hero.model.EffectiveStatus;
import com.xperience.hero.model.Event;

/**
 * Invitee-facing event summary. Deliberately excludes hostId and other invitees' data
 * (DESIGN.md Step 11: a leaked token must only expose that invitee's own view + public event info).
 */
public record PublicEventDto(
		String title,
		String description,
		Instant startTime,
		String location,
		Integer maxCapacity,
		long confirmedCount,
		EffectiveStatus status) {

	public static PublicEventDto from(Event event, long confirmedCount, Instant now) {
		return new PublicEventDto(
				event.getTitle(),
				event.getDescription(),
				event.getStartTime(),
				event.getLocation(),
				event.getMaxCapacity(),
				confirmedCount,
				event.effectiveStatus(now));
	}
}
