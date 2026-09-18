package com.xperience.hero.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.xperience.hero.dto.AttendeeDto;
import com.xperience.hero.dto.CountsDto;
import com.xperience.hero.dto.CreateEventRequest;
import com.xperience.hero.dto.DashboardDto;
import com.xperience.hero.dto.EventDto;
import com.xperience.hero.exception.ConflictException;
import com.xperience.hero.exception.ForbiddenException;
import com.xperience.hero.exception.NotFoundException;
import com.xperience.hero.model.ConfirmationState;
import com.xperience.hero.model.Event;
import com.xperience.hero.model.EventStatus;
import com.xperience.hero.model.Invitee;
import com.xperience.hero.model.RsvpResponse;
import com.xperience.hero.repository.EventRepository;
import com.xperience.hero.repository.InviteeRepository;

import lombok.RequiredArgsConstructor;

/** Host-facing event lifecycle: create, view own event, close, cancel (DESIGN.md Step 09). */
@Service
@RequiredArgsConstructor
public class EventService {

	private final EventRepository eventRepository;
	private final InviteeRepository inviteeRepository;

	@Transactional
	public Event createEvent(String hostId, CreateEventRequest request) {
		Event event = new Event();
		event.setHostId(hostId);
		event.setTitle(request.title());
		event.setDescription(request.description());
		event.setStartTime(request.startTime());
		event.setLocation(request.location());
		event.setMaxCapacity(request.maxCapacity());
		return eventRepository.save(event);
	}

	@Transactional(readOnly = true)
	public Event getForHost(UUID eventId, String hostId) {
		Event event = eventRepository.findById(eventId)
				.orElseThrow(() -> new NotFoundException("Event not found"));
		requireHost(event, hostId);
		return event;
	}

	@Transactional
	public Event closeEvent(UUID eventId, String hostId) {
		Event event = eventRepository.findByIdForUpdate(eventId)
				.orElseThrow(() -> new NotFoundException("Event not found"));
		requireHost(event, hostId);
		if (event.getStatus() == EventStatus.CANCELLED) {
			throw new ConflictException("Event is already cancelled");
		}
		event.setStatus(EventStatus.CLOSED);
		return event;
	}

	@Transactional
	public Event cancelEvent(UUID eventId, String hostId) {
		Event event = eventRepository.findByIdForUpdate(eventId)
				.orElseThrow(() -> new NotFoundException("Event not found"));
		requireHost(event, hostId);
		event.setStatus(EventStatus.CANCELLED);
		return event;
	}

	@Transactional(readOnly = true)
	public DashboardDto getDashboard(UUID eventId, String hostId) {
		Event event = getForHost(eventId, hostId);
		List<Invitee> invitees = inviteeRepository.findByEventIdOrderByInvitedAtAsc(eventId);

		long confirmed = 0, waitlisted = 0, no = 0, maybe = 0, pending = 0;
		List<AttendeeDto> attendees = invitees.stream().map(AttendeeDto::from).toList();
		for (Invitee invitee : invitees) {
			if (invitee.getResponse() == RsvpResponse.YES && invitee.getConfirmationState() == ConfirmationState.CONFIRMED) {
				confirmed++;
			} else if (invitee.getResponse() == RsvpResponse.YES && invitee.getConfirmationState() == ConfirmationState.WAITLISTED) {
				waitlisted++;
			} else if (invitee.getResponse() == RsvpResponse.NO) {
				no++;
			} else if (invitee.getResponse() == RsvpResponse.MAYBE) {
				maybe++;
			} else {
				pending++;
			}
		}

		Instant now = Instant.now();
		return new DashboardDto(EventDto.from(event, now), new CountsDto(confirmed, waitlisted, no, maybe, pending), attendees);
	}

	void requireHost(Event event, String hostId) {
		if (!event.getHostId().equals(hostId)) {
			throw new ForbiddenException("You are not the host of this event");
		}
	}
}
