package com.acme.payments;

/** The result of a successful charge. */
public final class Receipt {

    private final String orderId;
    private final long amountMinor;
    private final Currency currency;
    private final String authCode;

    public Receipt(String orderId, long amountMinor, Currency currency, String authCode) {
        this.orderId = orderId;
        this.amountMinor = amountMinor;
        this.currency = currency;
        this.authCode = authCode;
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
}
