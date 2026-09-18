package com.xperience.hero.dto;

import com.xperience.hero.model.RsvpResponse;

import jakarta.validation.constraints.NotNull;

public record RsvpRequest(@NotNull RsvpResponse response) {
}
