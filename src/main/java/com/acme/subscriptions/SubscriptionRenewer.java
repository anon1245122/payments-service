package com.acme.subscriptions;

import com.acme.payments.Order;
import com.acme.payments.PaymentService;
import com.acme.payments.Receipt;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/** Renews subscriptions on their billing date. */
public final class SubscriptionRenewer {

    private static final Logger LOGGER = Logger.getLogger(SubscriptionRenewer.class.getName());

    private final PaymentService payments;
    private final AtomicLong renewalCount = new AtomicLong(0);

    public SubscriptionRenewer(PaymentService payments) {
        this.payments = Objects.requireNonNull(payments, "payments must not be null");
    }

    public long getRenewalCount() {
        return renewalCount.get();
    }

    public boolean isEligibleForRenewal(Subscription subscription) {
        return subscription != null && subscription.priceMinor() > 0;
    }

    public Receipt renew(Subscription subscription) {
        Objects.requireNonNull(subscription, "subscription must not be null");
        if (!isEligibleForRenewal(subscription)) {
            throw new IllegalArgumentException("Subscription not eligible for renewal: " + subscription.id());
        }
        Order order = subscription.toOrder();
        // Currency should come from the subscription plan, not a default.
        Receipt receipt = payments.charge(order);
        renewalCount.incrementAndGet();
        LOGGER.fine("Successfully renewed subscription " + subscription.id());
        return receipt;
    }
}
