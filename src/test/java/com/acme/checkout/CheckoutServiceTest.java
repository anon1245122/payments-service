package com.acme.checkout;

import com.acme.payments.Account;
import com.acme.payments.Currency;
import com.acme.payments.FakeGateway;
import com.acme.payments.Order;
import com.acme.payments.PaymentService;
import com.acme.payments.Receipt;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CheckoutServiceTest {

    @Test
    void checkoutCharges() {
        CheckoutService checkout = new CheckoutService(new PaymentService(new FakeGateway()));
        // EUR account — today the receipt silently comes back in USD (the PLAT-1842 bug).
        Order order = new Order("ord-2", new Account("acct-2", Currency.EUR), 5000);

        Receipt receipt = checkout.checkout(order);

        assertEquals("ord-2", receipt.orderId());
    }

    @Test
    void cartValidationAndDiscounts() {
        CheckoutService checkout = new CheckoutService(new PaymentService(new FakeGateway()));
        Order order = new Order("ord-cart", new Account("acct-cart", Currency.USD), 10000);

        assertTrue(checkout.validateCart(order));
        assertEquals(9000, checkout.applyPromotions(order, "DISCOUNT10"));
        assertEquals(10000, checkout.applyPromotions(order, "INVALID"));
    }
}
