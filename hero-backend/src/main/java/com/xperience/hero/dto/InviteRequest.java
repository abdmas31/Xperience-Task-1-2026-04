package com.xperience.hero.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record InviteRequest(@NotEmpty @Size(max = 500) List<@NotEmpty String> emails) {
}
