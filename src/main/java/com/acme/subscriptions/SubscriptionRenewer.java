package com.acme.subscriptions;

import com.acme.payments.Order;
import com.acme.payments.PaymentService;
import com.acme.payments.Receipt;

/** Renews subscriptions on their billing date. */
public final class SubscriptionRenewer {

    private final PaymentService payments;

    public SubscriptionRenewer(PaymentService payments) {
        this.payments = payments;
    }

    public Receipt renew(Subscription subscription) {
        Order order = subscription.toOrder();
        // Currency should come from the subscription plan, not a default.
        return payments.charge(order);
    }
}
