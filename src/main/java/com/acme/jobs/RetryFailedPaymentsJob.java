package com.acme.jobs;

import com.acme.payments.Order;
import com.acme.payments.PaymentService;

import java.util.List;

/** Nightly batch that re-charges previously failed orders. */
public final class RetryFailedPaymentsJob {

    private final PaymentService payments;

    public RetryFailedPaymentsJob(PaymentService payments) {
        this.payments = payments;
    }

    public void retry(List<Order> failedOrders) {
        for (Order order : failedOrders) {
            // Must re-charge in the order's ORIGINAL currency, not a default.
            payments.charge(order);
        }
    }
}
