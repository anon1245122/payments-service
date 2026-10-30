package com.acme.payments;

/** A purchase order awaiting payment. */
public final class Order {

    private final String id;
    private final Account account;
    private final long amountMinor; // amount in minor units, e.g. cents

    public Order(String id, Account account, long amountMinor) {
        this.id = id;
        this.account = account;
        this.amountMinor = amountMinor;
    }

    public String id() {
        return id;
    }

    public Account account() {
        return account;
    }

    public long amountMinor() {
        return amountMinor;
    }
}
