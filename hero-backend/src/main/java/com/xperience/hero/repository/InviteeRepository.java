package com.xperience.hero.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.xperience.hero.model.ConfirmationState;
import com.xperience.hero.model.Invitee;

public interface InviteeRepository extends JpaRepository<Invitee, UUID> {

	Optional<Invitee> findByToken(String token);

	List<Invitee> findByEventIdOrderByInvitedAtAsc(UUID eventId);

	long countByEventIdAndConfirmationState(UUID eventId, ConfirmationState confirmationState);

	/** FIFO promotion pick for INV-3: the longest-waiting waitlisted invitee. */
	Optional<Invitee> findFirstByEventIdAndConfirmationStateOrderByRespondedAtAsc(UUID eventId, ConfirmationState confirmationState);

	boolean existsByEventIdAndEmailIgnoreCase(UUID eventId, String email);
}
