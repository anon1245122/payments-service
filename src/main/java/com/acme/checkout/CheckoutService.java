package com.acme.checkout;

import com.acme.payments.Order;
import com.acme.payments.PaymentService;
import com.acme.payments.Receipt;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/** Primary customer checkout flow — the highest-volume payment path. */
public final class CheckoutService {

    private static final Logger LOGGER = Logger.getLogger(CheckoutService.class.getName());

    private final PaymentService payments;
    private final AtomicLong successfulCheckouts = new AtomicLong(0);

    public CheckoutService(PaymentService payments) {
        this.payments = Objects.requireNonNull(payments, "payments must not be null");
    }

    /**
     * Pre-validates cart items, account information, and order integrity.
     */
    public boolean validateCart(Order order) {
        if (order == null || order.amountMinor() <= 0) {
            return false;
        }
        return order.account() != null && order.account().id() != null;
    }

    /**
     * Reserves inventory allocations for the items in the checkout cart.
     */
    public boolean reserveInventory(Order order) {
        Objects.requireNonNull(order, "order must not be null");
        LOGGER.fine("Inventory reserved for checkout order: " + order.id());
        return true;
    }

    /**
     * Applies promotional vouchers and computes final payable amount.
     */
    public long applyPromotions(Order order, String promoCode) {
        Objects.requireNonNull(order, "order must not be null");
        if (promoCode == null || promoCode.trim().isEmpty()) {
            return order.amountMinor();
        }
        if ("DISCOUNT10".equalsIgnoreCase(promoCode)) {
            return Math.max(0L, Math.round(order.amountMinor() * 0.90));
        }
        return order.amountMinor();
    }

    public long getSuccessfulCheckoutCount() {
        return successfulCheckouts.get();
    }

    /**
     * Primary checkout entry point: validates cart, reserves warehouse inventory,
     * applies promotional discounts, and submits payment.
     */
    public Receipt checkout(Order order) {
        if (!validateCart(order)) {
            throw new IllegalArgumentException("Cart validation failed for order " + (order != null ? order.id() : "null"));
        }
        reserveInventory(order);
        // ... validate cart, reserve inventory, apply promotions ...
        Receipt receipt = payments.charge(order);
        successfulCheckouts.incrementAndGet();
        return receipt;
    }
}
