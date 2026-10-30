package com.acme.payments;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PaymentServiceTest {

    @Test
    void chargesOrder() {
        PaymentService payments = new PaymentService(new FakeGateway());
        Account account = new Account("acct-1", Currency.USD);
        Order order = new Order("ord-1", account, 1999);

        Receipt receipt = payments.charge(order);

        assertEquals("ord-1", receipt.orderId());
        assertEquals(1999, receipt.amountMinor());
        assertEquals(Currency.USD, receipt.currency());
    }
}
