package com.acme.payments;

/**
 * Low-level payment gateway integration interface that communicates
 * with external payment networks and merchant settlement providers.
 */
public interface PaymentGateway {

    /**
     * Submits a direct charge request for immediate settlement.
     */
    Receipt submit(String orderId, long amountMinor, Currency currency);

    /**
     * Pre-authorizes an amount against a payment credential without capturing funds.
     */
    Receipt authorize(String orderId, long amountMinor, Currency currency);

    /**
     * Captures a previously authorized charge.
     */
    Receipt capture(String authCode, long amountMinor);

    /**
     * Voids an active authorization before capture.
     */
    boolean voidAuthorization(String authCode);

    /**
     * Submits a refund request for a previously settled charge.
     */
    Receipt refund(String orderId, long amountMinor, String reason);

    /**
     * Checks gateway network availability and service health.
     */
    boolean ping();
}
