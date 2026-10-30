package com.acme.payments;

/** A customer billing account. */
public final class Account {

    private final String id;
    private final Currency billingCurrency;

    public Account(String id, Currency billingCurrency) {
        this.id = id;
        this.billingCurrency = billingCurrency;
    }

    public String id() {
        return id;
    }

    /** The currency this account is billed in. */
    public Currency billingCurrency() {
        return billingCurrency;
    }
}
