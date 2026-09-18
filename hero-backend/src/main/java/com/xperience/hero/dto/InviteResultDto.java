package com.xperience.hero.dto;

import java.util.UUID;

import com.xperience.hero.model.Invitee;

/** One created invite, including the invitee-facing link — the host copies/sends this themselves (DESIGN.md OQ2). */
public record InviteResultDto(UUID id, String email, String token, String rsvpPath) {

	public static InviteResultDto from(Invitee invitee) {
		return new InviteResultDto(invitee.getId(), invitee.getEmail(), invitee.getToken(), "/rsvp/" + invitee.getToken());
	}
}
