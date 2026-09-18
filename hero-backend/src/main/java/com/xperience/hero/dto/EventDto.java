package com.xperience.hero.dto;

import java.time.Instant;
import java.util.UUID;

import com.xperience.hero.model.EffectiveStatus;
import com.xperience.hero.model.Event;

public record EventDto(
		UUID id,
		String title,
		String description,
		Instant startTime,
		String location,
		Integer maxCapacity,
		EffectiveStatus status,
		Instant createdAt) {

	public static EventDto from(Event event, Instant now) {
		return new EventDto(
				event.getId(),
				event.getTitle(),
				event.getDescription(),
				event.getStartTime(),
				event.getLocation(),
				event.getMaxCapacity(),
				event.effectiveStatus(now),
				event.getCreatedAt());
	}
}
