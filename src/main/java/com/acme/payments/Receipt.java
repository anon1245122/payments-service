package com.acme.payments;

import java.time.Instant;
import java.util.Objects;

/**
 * The record and confirmation of a payment operation (charge, authorization, capture, or refund).
 */
public final class Receipt {

    private final String orderId;
    private final long amountMinor;
    private final Currency currency;
    private final String authCode;
    private final Instant createdAt;

    public Receipt(String orderId, long amountMinor, Currency currency, String authCode) {
        this(orderId, amountMinor, currency, authCode, Instant.now());
    }

    public Receipt(String orderId, long amountMinor, Currency currency, String authCode, Instant createdAt) {
        this.orderId = Objects.requireNonNull(orderId, "orderId must not be null");
        this.amountMinor = amountMinor;
        this.currency = Objects.requireNonNull(currency, "currency must not be null");
        this.authCode = Objects.requireNonNull(authCode, "authCode must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public String orderId() {
        return orderId;
    }

    public long amountMinor() {
        return amountMinor;
    }

    public Currency currency() {
        return currency;
    }

    public String authCode() {
        return authCode;
    }

    public Instant createdAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Receipt other)) return false;
        return amountMinor == other.amountMinor &&
                Objects.equals(orderId, other.orderId) &&
                currency == other.currency &&
                Objects.equals(authCode, other.authCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderId, amountMinor, currency, authCode);
    }

    @Override
    public String toString() {
        return "Receipt{" +
                "orderId='" + orderId + '\'' +
                ", amountMinor=" + amountMinor +
                ", currency=" + currency +
                ", authCode='" + authCode + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
