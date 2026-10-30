package com.acme.payments;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PaymentServiceTest {

    private FakeGateway gateway;
    private PaymentService payments;

    @BeforeEach
    void setUp() {
        gateway = new FakeGateway();
        payments = new PaymentService(gateway);
    }

    @Test
    void chargesOrder() {
        Account account = new Account("acct-1", Currency.USD);
        Order order = new Order("ord-1", account, 1999);

        Receipt receipt = payments.charge(order);

        assertEquals("ord-1", receipt.orderId());
        assertEquals(1999, receipt.amountMinor());
        assertEquals(Currency.USD, receipt.currency());
    }

    @Test
    void authorizesAndCapturesOrder() {
        Account account = new Account("acct-2", Currency.USD);
        Order order = new Order("ord-auth", account, 5000);

        Receipt auth = payments.authorize(order);
        assertNotNull(auth.authCode());
        assertEquals("ord-auth", auth.orderId());

        Receipt capture = payments.capture(auth.authCode(), 5000);
        assertEquals("ord-auth", capture.orderId());
        assertEquals(5000, capture.amountMinor());
    }

    @Test
    void voidsAuthorization() {
        Account account = new Account("acct-3", Currency.USD);
        Order order = new Order("ord-void", account, 2500);

        Receipt auth = payments.authorize(order);
        assertTrue(payments.voidAuthorization(auth.authCode()));
        assertFalse(payments.voidAuthorization(auth.authCode()));
    }

    @Test
    void validatesOrderConstraints() {
        Account account = new Account("acct-4", Currency.USD);

        assertThrows(IllegalArgumentException.class, () ->
                payments.validateOrder(new Order("", account, 1000)));

        assertThrows(IllegalArgumentException.class, () ->
                payments.validateOrder(new Order("ord-min", account, 10)));

        assertThrows(IllegalArgumentException.class, () ->
                payments.validateOrder(new Order("ord-max", account, 50_000_000L)));
    }

    @Test
    void calculatesFeesAndFx() {
        long feeDomestic = payments.calculateProcessingFee(10000, false);
        long feeInternational = payments.calculateProcessingFee(10000, true);
        assertTrue(feeInternational > feeDomestic);

        assertEquals(1.0, payments.estimateExchangeRate(Currency.USD, Currency.USD));
        assertTrue(payments.estimateExchangeRate(Currency.USD, Currency.JPY) > 1.0);
    }

    @Test
    void checksHealthAndOperationalStatus() {
        assertTrue(payments.checkHealth());
        assertTrue(payments.isOperatingNormally());

        payments.setOperationalStatus(false);
        assertFalse(payments.checkHealth());
        assertFalse(payments.isOperatingNormally());
    }
}
