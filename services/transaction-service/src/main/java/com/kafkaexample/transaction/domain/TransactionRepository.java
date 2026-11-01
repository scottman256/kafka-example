package com.kafkaexample.transaction.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

	@Query("""
			select new com.kafkaexample.transaction.domain.TransactionWithPublishLog(transaction, publishLogEntry)
			from Transaction transaction
			left join KafkaPublishLogEntry publishLogEntry on publishLogEntry.transactionId = transaction.id
			order by transaction.createdAt desc
			""")
	List<TransactionWithPublishLog> findRecentWithPublishLog(Pageable pageable);

	@Query("""
			select new com.kafkaexample.transaction.domain.TransactionWithPublishLog(transaction, publishLogEntry)
			from Transaction transaction
			left join KafkaPublishLogEntry publishLogEntry on publishLogEntry.transactionId = transaction.id
			where transaction.id = :id
			""")
	Optional<TransactionWithPublishLog> findWithPublishLogById(UUID id);

}
