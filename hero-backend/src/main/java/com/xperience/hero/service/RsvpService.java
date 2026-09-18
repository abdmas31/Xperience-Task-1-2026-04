package com.xperience.hero.service;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.xperience.hero.dto.PublicEventDto;
import com.xperience.hero.dto.RsvpViewDto;
import com.xperience.hero.exception.BadRequestException;
import com.xperience.hero.exception.ConflictException;
import com.xperience.hero.exception.NotFoundException;
import com.xperience.hero.model.ConfirmationState;
import com.xperience.hero.model.Event;
import com.xperience.hero.model.Invitee;
import com.xperience.hero.model.RsvpResponse;
import com.xperience.hero.repository.EventRepository;
import com.xperience.hero.repository.InviteeRepository;

import lombok.RequiredArgsConstructor;

/**
 * Owns capacity + waitlist logic end to end (DESIGN.md Step 09: one code path for INV-1/INV-3,
 * not duplicated). See DESIGN.md Step 12 for the concurrency reasoning behind the locking here.
 */
@Service
@RequiredArgsConstructor
public class RsvpService {

	private final EventRepository eventRepository;
	private final InviteeRepository inviteeRepository;

	@Transactional(readOnly = true)
	public RsvpViewDto getByToken(String token) {
		Invitee invitee = findByToken(token);
		Event event = invitee.getEvent();
		long confirmedCount = inviteeRepository.countByEventIdAndConfirmationState(event.getId(), ConfirmationState.CONFIRMED);
		return RsvpViewDto.of(invitee, PublicEventDto.from(event, confirmedCount, Instant.now()));
	}

	@Transactional
	public RsvpViewDto respond(String token, RsvpResponse newResponse) {
		if (newResponse == null || newResponse == RsvpResponse.PENDING) {
			throw new BadRequestException("response must be one of YES, NO, MAYBE");
		}

		Invitee invitee = findByToken(token);

		// Lock the event row first (DESIGN.md Step 12) so a concurrent response to the same event
		// serializes behind this one — the capacity check below always sees a committed, up-to-date count.
		Event event = eventRepository.findByIdForUpdate(invitee.getEvent().getId())
				.orElseThrow(() -> new NotFoundException("Event not found"));

		Instant now = Instant.now();
		if (!event.acceptsResponses(now)) {
			throw new ConflictException("This event is no longer accepting RSVP changes");
		}

		if (invitee.getResponse() == newResponse) {
			// Idempotent no-op (DESIGN.md Step 12): a repeat submission of the same response,
			// including a Yes-while-waitlisted re-submit, never re-runs the capacity check.
			long confirmedCount = inviteeRepository.countByEventIdAndConfirmationState(event.getId(), ConfirmationState.CONFIRMED);
			return RsvpViewDto.of(invitee, PublicEventDto.from(event, confirmedCount, now));
		}

		ConfirmationState oldConfirmation = invitee.getConfirmationState();
		ConfirmationState newConfirmation;

		if (newResponse == RsvpResponse.YES) {
			long confirmedCount = inviteeRepository.countByEventIdAndConfirmationState(event.getId(), ConfirmationState.CONFIRMED);
			boolean hasRoom = event.getMaxCapacity() == null || confirmedCount < event.getMaxCapacity();
			newConfirmation = hasRoom ? ConfirmationState.CONFIRMED : ConfirmationState.WAITLISTED;
		} else {
			newConfirmation = ConfirmationState.NONE;
		}

		invitee.setResponse(newResponse);
		invitee.setConfirmationState(newConfirmation);
		invitee.setRespondedAt(now);
		inviteeRepository.save(invitee);

		// INV-3: any transition out of Yes-confirmed frees a seat and must promote in the same
		// transaction (A3 in DESIGN.md — generalized beyond the brief's literal "changes to No").
		if (oldConfirmation == ConfirmationState.CONFIRMED && newConfirmation != ConfirmationState.CONFIRMED) {
			promoteNextWaitlisted(event);
		}

		long confirmedCount = inviteeRepository.countByEventIdAndConfirmationState(event.getId(), ConfirmationState.CONFIRMED);
		return RsvpViewDto.of(invitee, PublicEventDto.from(event, confirmedCount, now));
	}

	private void promoteNextWaitlisted(Event event) {
		inviteeRepository.findFirstByEventIdAndConfirmationStateOrderByRespondedAtAsc(event.getId(), ConfirmationState.WAITLISTED)
				.ifPresent(promoted -> {
					promoted.setConfirmationState(ConfirmationState.CONFIRMED);
					inviteeRepository.save(promoted);
				});
	}

	private Invitee findByToken(String token) {
		return inviteeRepository.findByToken(token)
				.orElseThrow(() -> new NotFoundException("Invalid RSVP link"));
	}
}
