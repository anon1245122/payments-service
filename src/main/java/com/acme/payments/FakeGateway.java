package com.acme.payments;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory simulated payment gateway used in tests and local development runs.
 */
public final class FakeGateway implements PaymentGateway {

    private final Map<String, Receipt> authorizations = new ConcurrentHashMap<>();
    private final Map<String, Receipt> settledCharges = new ConcurrentHashMap<>();
    private volatile boolean operational = true;

    @Override
    public Receipt submit(String orderId, long amountMinor, Currency currency) {
        Receipt receipt = new Receipt(orderId, amountMinor, currency, "AUTH-" + orderId);
        settledCharges.put(orderId, receipt);
        return receipt;
    }

    @Override
    public Receipt authorize(String orderId, long amountMinor, Currency currency) {
        Receipt auth = new Receipt(orderId, amountMinor, currency, "AUTH-PRE-" + orderId);
        authorizations.put(auth.authCode(), auth);
        return auth;
    }

    @Override
    public Receipt capture(String authCode, long amountMinor) {
        Receipt auth = authorizations.get(authCode);
        Currency curr = auth != null ? auth.currency() : Currency.USD;
        String orderId = auth != null ? auth.orderId() : "cap-" + authCode;
        Receipt captureReceipt = new Receipt(orderId, amountMinor, curr, "CAP-" + authCode);
        settledCharges.put(orderId, captureReceipt);
        return captureReceipt;
    }

    @Override
    public boolean voidAuthorization(String authCode) {
        return authorizations.remove(authCode) != null;
    }

    @Override
    public Receipt refund(String orderId, long amountMinor, String reason) {
        Receipt original = settledCharges.get(orderId);
        Currency curr = original != null ? original.currency() : Currency.USD;
        return new Receipt(orderId, amountMinor, curr, "REF-" + orderId);
    }

    @Override
    public boolean ping() {
        return operational;
    }

    public void setOperational(boolean operational) {
        this.operational = operational;
    }

    public void reset() {
        authorizations.clear();
        settledCharges.clear();
        operational = true;
    }
}
