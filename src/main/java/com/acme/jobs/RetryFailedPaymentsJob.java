package com.acme.jobs;

import com.acme.payments.Order;
import com.acme.payments.PaymentService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Nightly batch that re-charges previously failed orders. */
public final class RetryFailedPaymentsJob {

    private static final Logger LOGGER = Logger.getLogger(RetryFailedPaymentsJob.class.getName());

    public static final int MAX_BATCH_SIZE = 500;

    private final PaymentService payments;
    private final List<String> deadLetterQueue = Collections.synchronizedList(new ArrayList<>());
    private final AtomicInteger successfulRetries = new AtomicInteger(0);
    private final AtomicInteger failedRetries = new AtomicInteger(0);

    public RetryFailedPaymentsJob(PaymentService payments) {
        this.payments = Objects.requireNonNull(payments, "payments must not be null");
    }

    public int getSuccessfulRetryCount() {
        return successfulRetries.get();
    }

    public int getFailedRetryCount() {
        return failedRetries.get();
    }

    public List<String> getDeadLetterQueue() {
        return Collections.unmodifiableList(new ArrayList<>(deadLetterQueue));
    }

    public void retry(List<Order> failedOrders) {
        if (failedOrders == null || failedOrders.isEmpty()) {
            return;
        }

        for (Order order : failedOrders) {
            try {
                // Must re-charge in the order's ORIGINAL currency, not a default.
                payments.charge(order);
                successfulRetries.incrementAndGet();
            } catch (Exception ex) {
                failedRetries.incrementAndGet();
                deadLetterQueue.add(order.id());
                LOGGER.log(Level.WARNING, "Retry attempt failed for order " + order.id(), ex);
            }
        }
    }
}
