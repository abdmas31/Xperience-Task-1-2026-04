package com.xperience.hero.model;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Invitee entity (DESIGN.md Step 10). An RSVP is 1:1 with an invitee, so response/confirmation
 * state live directly here rather than in a separate Rsvp table (no history requirement, Step 15).
 */
@Entity
@Table(name = "invitees", uniqueConstraints = @UniqueConstraint(columnNames = { "event_id", "email" }))
@Getter
@Setter
@NoArgsConstructor
public class Invitee {

	@Id
	@GeneratedValue
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "event_id", nullable = false, updatable = false)
	private Event event;

	@Column(nullable = false, updatable = false)
	private String email;

	/** Unguessable bearer credential for the invitee-facing link (DESIGN.md INV-5, Step 11). Distinct from {@code id}. */
	@Column(nullable = false, unique = true, updatable = false)
	private String token = UUID.randomUUID().toString();

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private RsvpResponse response = RsvpResponse.PENDING;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ConfirmationState confirmationState = ConfirmationState.NONE;

	/** Timestamp of the current response; also the FIFO ordering key for waitlist promotion (A4, INV-3). */
	private Instant respondedAt;

	@Column(nullable = false, updatable = false)
	private Instant invitedAt = Instant.now();
}
