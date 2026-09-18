package com.xperience.hero.controller;

import java.time.Instant;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.xperience.hero.dto.CreateEventRequest;
import com.xperience.hero.dto.DashboardDto;
import com.xperience.hero.dto.EventDto;
import com.xperience.hero.model.Event;
import com.xperience.hero.service.EventService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

/** Host-facing endpoints. Trust boundary: every method requires X-Host-Id and enforces INV-6 ownership (DESIGN.md Step 11). */
@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

	private final EventService eventService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public EventDto createEvent(@RequestHeader("X-Host-Id") String hostId, @Valid @RequestBody CreateEventRequest request) {
		Event event = eventService.createEvent(hostId, request);
		return EventDto.from(event, Instant.now());
	}

	@GetMapping("/{eventId}")
	public DashboardDto getDashboard(@RequestHeader("X-Host-Id") String hostId, @PathVariable UUID eventId) {
		return eventService.getDashboard(eventId, hostId);
	}

	@PostMapping("/{eventId}/close")
	public EventDto closeEvent(@RequestHeader("X-Host-Id") String hostId, @PathVariable UUID eventId) {
		Event event = eventService.closeEvent(eventId, hostId);
		return EventDto.from(event, Instant.now());
	}

	@PostMapping("/{eventId}/cancel")
	public EventDto cancelEvent(@RequestHeader("X-Host-Id") String hostId, @PathVariable UUID eventId) {
		Event event = eventService.cancelEvent(eventId, hostId);
		return EventDto.from(event, Instant.now());
	}
}
