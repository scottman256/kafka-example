package com.kafkaexample.ledger.accounting;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface AccountRepository extends JpaRepository<Account, UUID> {

	Optional<Account> findByNormalizedName(String normalizedName);

	/** Holds a row lock until the transaction ends, so concurrent postings to one account queue up. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<Account> findForUpdateByNormalizedName(String normalizedName);

	List<Account> findAllByOrderByNormalizedNameAsc();

	@Query("""
			select new com.kafkaexample.ledger.accounting.TrialBalance(count(account),
			  sum(case when account.balance < 0 then -account.balance else 0 end),
			  sum(case when account.balance > 0 then account.balance else 0 end))
			from Account account
			""")
	TrialBalance calculateTrialBalance();

}
