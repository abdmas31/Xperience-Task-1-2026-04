package com.xperience.hero.dto;

import java.time.Instant;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateEventRequest(
		@NotBlank @Size(max = 200) String title,
		@Size(max = 2000) String description,
		@NotNull @Future Instant startTime,
		@NotBlank @Size(max = 300) String location,
		@Positive Integer maxCapacity) {
}
