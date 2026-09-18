package com.xperience.hero.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.xperience.hero.dto.InviteRequest;
import com.xperience.hero.dto.InviteResultDto;
import com.xperience.hero.model.Invitee;
import com.xperience.hero.service.InvitationService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/events/{eventId}/invitees")
@RequiredArgsConstructor
public class InvitationController {

	private final InvitationService invitationService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public List<InviteResultDto> invite(
			@RequestHeader("X-Host-Id") String hostId,
			@PathVariable UUID eventId,
			@Valid @RequestBody InviteRequest request) {
		List<Invitee> created = invitationService.invite(eventId, hostId, request.emails());
		return created.stream().map(InviteResultDto::from).toList();
	}
}
