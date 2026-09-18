package com.xperience.hero.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.xperience.hero.model.Event;

import jakarta.persistence.LockModeType;

public interface EventRepository extends JpaRepository<Event, UUID> {

	/**
	 * Row-locking read used by every RSVP-affecting write path (DESIGN.md Step 12): takes a
	 * PESSIMISTIC_WRITE lock on the event row so concurrent responses to the same event serialize,
	 * making the "two simultaneous last-seat Yes responses" race impossible by construction.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select e from Event e where e.id = :id")
	Optional<Event> findByIdForUpdate(@Param("id") UUID id);
}
