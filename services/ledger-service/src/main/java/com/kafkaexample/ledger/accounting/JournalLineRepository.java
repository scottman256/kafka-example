package com.kafkaexample.ledger.accounting;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JournalLineRepository extends JpaRepository<JournalLine, UUID> {

	@EntityGraph(attributePaths = "journalEntry")
	List<JournalLine> findByAccountOrderByJournalEntryPostedAtDesc(Account account, Limit limit);

}
