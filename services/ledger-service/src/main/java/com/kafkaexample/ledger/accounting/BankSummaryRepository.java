package com.kafkaexample.ledger.accounting;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface BankSummaryRepository extends JpaRepository<BankSummary, UUID> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<BankSummary> findForUpdateByBankName(String bankName);

	List<BankSummary> findAllByOrderByBankNameAsc();

}
