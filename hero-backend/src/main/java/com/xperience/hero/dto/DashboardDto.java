package com.xperience.hero.dto;

import java.util.List;

public record DashboardDto(EventDto event, CountsDto counts, List<AttendeeDto> attendees) {
}
