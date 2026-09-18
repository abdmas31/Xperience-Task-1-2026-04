package com.xperience.hero.dto;

import java.time.Instant;
import java.util.UUID;

import com.xperience.hero.model.ConfirmationState;
import com.xperience.hero.model.Invitee;
import com.xperience.hero.model.RsvpResponse;

/** Host-facing view of one invitee. Includes the token so the host can (re)share the link, since invite delivery is out of scope (DESIGN.md OQ2). */
public record AttendeeDto(
		UUID id,
		String email,
		String token,
		RsvpResponse response,
		ConfirmationState confirmationState,
		Instant invitedAt,
		Instant respondedAt) {

	public static AttendeeDto from(Invitee invitee) {
		return new AttendeeDto(
				invitee.getId(),
				invitee.getEmail(),
				invitee.getToken(),
				invitee.getResponse(),
				invitee.getConfirmationState(),
				invitee.getInvitedAt(),
				invitee.getRespondedAt());
	}
}
