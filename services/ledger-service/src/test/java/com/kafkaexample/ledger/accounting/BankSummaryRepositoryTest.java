package com.kafkaexample.ledger.accounting;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class BankSummaryRepositoryTest {

	@Autowired
	private BankSummaryRepository bankSummaryRepository;

	@Test
	void findForUpdateByBankNameLocksAndReturnsTheSummary() {
		BankSummary saved = bankSummaryRepository.saveAndFlush(new BankSummary("Pioneer Trust"));

		assertThat(bankSummaryRepository.findForUpdateByBankName("Pioneer Trust")).contains(saved);
		assertThat(bankSummaryRepository.findForUpdateByBankName("Bank of Nowhere")).isEmpty();
	}

	@Test
	void findAllByOrderByBankNameAscListsBanksAlphabetically() {
		BankSummary summit = bankSummaryRepository.save(new BankSummary("Summit Credit Union"));
		BankSummary evergreen = bankSummaryRepository.save(new BankSummary("Evergreen Community Bank"));

		assertThat(bankSummaryRepository.findAllByOrderByBankNameAsc()).containsExactly(evergreen, summit);
	}

	@Test
	void eachBankHasOneSummary() {
		bankSummaryRepository.saveAndFlush(new BankSummary("Pioneer Trust"));

		assertThatThrownBy(() -> bankSummaryRepository.saveAndFlush(new BankSummary("Pioneer Trust")))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

}
