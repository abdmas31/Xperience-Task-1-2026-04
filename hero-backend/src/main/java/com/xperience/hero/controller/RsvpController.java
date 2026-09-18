package com.xperience.hero.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.xperience.hero.dto.RsvpRequest;
import com.xperience.hero.dto.RsvpViewDto;
import com.xperience.hero.service.RsvpService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

/** Invitee-facing endpoints. Trust boundary: unauthenticated, identified only by the token (DESIGN.md Step 11). */
@RestController
@RequestMapping("/api/rsvp/{token}")
@RequiredArgsConstructor
public class RsvpController {

	private final RsvpService rsvpService;

	@GetMapping
	public RsvpViewDto get(@PathVariable String token) {
		return rsvpService.getByToken(token);
	}

	@PostMapping
	public RsvpViewDto respond(@PathVariable String token, @Valid @RequestBody RsvpRequest request) {
		return rsvpService.respond(token, request.response());
	}
}
