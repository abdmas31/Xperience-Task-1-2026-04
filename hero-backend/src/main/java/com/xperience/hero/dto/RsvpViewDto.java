package com.xperience.hero.dto;

import java.time.Instant;

import com.xperience.hero.model.ConfirmationState;
import com.xperience.hero.model.Invitee;
import com.xperience.hero.model.RsvpResponse;

public record RsvpViewDto(
		PublicEventDto event,
		String email,
		RsvpResponse response,
		ConfirmationState confirmationState,
		Instant respondedAt) {

	public static RsvpViewDto of(Invitee invitee, PublicEventDto eventDto) {
		return new RsvpViewDto(
				eventDto,
				invitee.getEmail(),
				invitee.getResponse(),
				invitee.getConfirmationState(),
				invitee.getRespondedAt());
	}
}
