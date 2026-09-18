package com.xperience.hero.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.xperience.hero.exception.BadRequestException;
import com.xperience.hero.exception.ConflictException;
import com.xperience.hero.exception.ForbiddenException;
import com.xperience.hero.exception.NotFoundException;
import com.xperience.hero.model.Event;
import com.xperience.hero.model.Invitee;
import com.xperience.hero.repository.EventRepository;
import com.xperience.hero.repository.InviteeRepository;

import lombok.RequiredArgsConstructor;

/** Host-facing invitation workflow, kept separate from RsvpService: different trust boundary (DESIGN.md Step 09/11). */
@Service
@RequiredArgsConstructor
public class InvitationService {

	private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

	private final EventRepository eventRepository;
	private final InviteeRepository inviteeRepository;

	@Transactional
	public List<Invitee> invite(UUID eventId, String hostId, List<String> rawEmails) {
		Event event = eventRepository.findById(eventId)
				.orElseThrow(() -> new NotFoundException("Event not found"));
		if (!event.getHostId().equals(hostId)) {
			throw new ForbiddenException("You are not the host of this event");
		}
		if (!event.acceptsResponses(Instant.now())) {
			throw new ConflictException("Cannot invite people to an event that is closed, cancelled, or already started");
		}

		List<Invitee> created = new ArrayList<>();
		for (String raw : rawEmails) {
			String email = raw == null ? "" : raw.trim().toLowerCase();
			if (email.isEmpty()) {
				continue;
			}
			if (!EMAIL_PATTERN.matcher(email).matches()) {
				throw new BadRequestException("Invalid email address: " + raw);
			}
			if (inviteeRepository.existsByEventIdAndEmailIgnoreCase(eventId, email)) {
				throw new ConflictException("Already invited: " + email);
			}

			Invitee invitee = new Invitee();
			invitee.setEvent(event);
			invitee.setEmail(email);
			created.add(inviteeRepository.save(invitee));
		}
		return created;
	}
}
