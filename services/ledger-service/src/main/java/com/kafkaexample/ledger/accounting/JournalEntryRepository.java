package com.kafkaexample.ledger.accounting;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JournalEntryRepository extends JpaRepository<JournalEntry, UUID> {

	boolean existsByEventId(UUID eventId);

}
