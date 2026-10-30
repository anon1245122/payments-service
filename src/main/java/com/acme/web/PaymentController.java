package com.acme.web;

import com.acme.payments.Order;
import com.acme.payments.PaymentService;
import com.acme.payments.Receipt;

import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

/** HTTP endpoint for ad-hoc payments. */
public final class PaymentController {

    private static final Logger LOGGER = Logger.getLogger(PaymentController.class.getName());

    private final PaymentService payments;

    public PaymentController(PaymentService payments) {
        this.payments = Objects.requireNonNull(payments, "payments must not be null");
    }

    /**
     * GET /api/payments/health
     */
    public boolean health() {
        return payments.checkHealth();
    }

    /**
     * GET /api/payments/receipts/{orderId}
     */
    public Optional<Receipt> getReceipt(String orderId) {
        Objects.requireNonNull(orderId, "orderId must not be null");
        return payments.findReceipt(orderId);
    }

    /**
     * POST /api/payments/refund
     */
    public Receipt refund(String orderId, long amountMinor, String reason) {
        Objects.requireNonNull(orderId, "orderId must not be null");
        LOGGER.info("Processing refund endpoint for order " + orderId);
        return payments.refund(orderId, amountMinor, reason);
    }

    /**
     * POST /api/payments/pay
     */
    public Receipt pay(Order order) {
        Objects.requireNonNull(order, "order must not be null");
        LOGGER.info("Processing payment endpoint for order " + order.id());
        return payments.charge(order);
    }
}
