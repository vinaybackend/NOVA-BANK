package com.nova.bank.account;

import com.nova.bank.account.entities.Account;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class AccountServiceApplicationTests {

	@Test
	void contextLoads() {
	}

	@Test
	void newlyIssuedVirtualCardPreviewHasExpiryAndMaskedReference() {
		var account = new Account();

		account.issueDebitCardPreviewIfMissing();

		assertEquals("PREVIEW", account.getDebitCardStatus());
		assertEquals("VIRTUAL", account.getDebitCardType());
		assertTrue(account.getDebitCardNumber().startsWith("NVC-"));
		assertNotNull(account.getDebitCardIssuedAt());
		assertEquals(4, account.getDebitCardLastFourDigits().length());
		assertTrue(account.getDebitCardExpiryDate().isAfter(LocalDate.now()));
		assertEquals(YearMonth.now().plusYears(5), YearMonth.from(account.getDebitCardExpiryDate()));
	}

	@Test
	void issuingPreviewAgainDoesNotReplaceExistingReference() {
		var account = new Account();
		account.issueDebitCardPreviewIfMissing();
		String firstReference = account.getDebitCardLastFourDigits();
		var firstIssuedAt = account.getDebitCardIssuedAt();

		account.issueDebitCardPreviewIfMissing();

		assertEquals(firstReference, account.getDebitCardLastFourDigits());
		assertEquals(firstIssuedAt, account.getDebitCardIssuedAt());
	}
}
