package com.acme.admin;

import com.acme.payments.Order;
import com.acme.payments.PaymentService;
import com.acme.payments.Receipt;

/** Internal support tool for manually charging an order. Low traffic. */
public final class ManualChargeTool {

    private final PaymentService payments;

    public ManualChargeTool(PaymentService payments) {
        this.payments = payments;
    }

    public Receipt forceCharge(Order order) {
        return payments.charge(order);
    }
}
