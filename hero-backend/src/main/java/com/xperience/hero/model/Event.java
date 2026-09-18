package com.xperience.hero.model;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Event entity (DESIGN.md Step 10). Ownership: every field here is written only by the host,
 * except {@code status}, which is written by the host's close/cancel actions.
 */
@Entity
@Table(name = "events")
@Getter
@Setter
@NoArgsConstructor
public class Event {

	@Id
	@GeneratedValue
	private UUID id;

	/** Stubbed host identity (DESIGN.md OQ1) — an opaque caller-supplied id, not a real auth principal. */
	@Column(nullable = false, updatable = false)
	private String hostId;

	@Column(nullable = false)
	private String title;

	@Column(length = 2000)
	private String description;

	@Column(nullable = false)
	private Instant startTime;

	@Column(nullable = false)
	private String location;

	/** Null means uncapped (DESIGN.md A6). */
	private Integer maxCapacity;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private EventStatus status = EventStatus.SCHEDULED;

	@Column(nullable = false, updatable = false)
	private Instant createdAt = Instant.now();

	/**
	 * INV-2/INV-7 gate: an event only accepts new/changed RSVPs — and new invitations — while
	 * SCHEDULED and before its start time.
	 */
	public boolean acceptsResponses(Instant now) {
		return status == EventStatus.SCHEDULED && now.isBefore(startTime);
	}

	public EffectiveStatus effectiveStatus(Instant now) {
		if (status == EventStatus.CANCELLED) {
			return EffectiveStatus.CANCELLED;
		}
		if (!now.isBefore(startTime)) {
			return EffectiveStatus.LOCKED;
		}
		if (status == EventStatus.CLOSED) {
			return EffectiveStatus.CLOSED;
		}
		return EffectiveStatus.OPEN;
	}
}
