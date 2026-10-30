package com.acme.payments;

import java.util.Objects;

/** A purchase order awaiting payment. */
public final class Order {

    private final String id;
    private final Account account;
    private final long amountMinor; // amount in minor units, e.g. cents

    public Order(String id, Account account, long amountMinor) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.account = Objects.requireNonNull(account, "account must not be null");
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Order other)) return false;
        return amountMinor == other.amountMinor &&
                Objects.equals(id, other.id) &&
                Objects.equals(account, other.account);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, account, amountMinor);
    }

    @Override
    public String toString() {
        return "Order[id=" + id + ", account=" + account.id() + ", amountMinor=" + amountMinor + "]";
    }
}
