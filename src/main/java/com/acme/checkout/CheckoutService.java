package com.acme.checkout;

import com.acme.payments.Order;
import com.acme.payments.PaymentService;
import com.acme.payments.Receipt;

/** Primary customer checkout flow — the highest-volume payment path. */
public final class CheckoutService {

    private final PaymentService payments;

    public CheckoutService(PaymentService payments) {
        this.payments = payments;
    }

    public Receipt checkout(Order order) {
        // ... validate cart, reserve inventory, apply promotions ...
        return payments.charge(order);
    }
}
