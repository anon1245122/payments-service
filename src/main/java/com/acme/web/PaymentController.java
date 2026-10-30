package com.acme.web;

import com.acme.payments.Order;
import com.acme.payments.PaymentService;
import com.acme.payments.Receipt;

/** HTTP endpoint for ad-hoc payments. */
public final class PaymentController {

    private final PaymentService payments;

    public PaymentController(PaymentService payments) {
        this.payments = payments;
    }

    public Receipt pay(Order order) {
        return payments.charge(order);
    }
}
