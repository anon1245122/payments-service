package com.acme.payments;

import java.util.Objects;

/** A customer billing account. */
public final class Account {

    private final String id;
    private final Currency billingCurrency;

    public Account(String id, Currency billingCurrency) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.billingCurrency = Objects.requireNonNull(billingCurrency, "billingCurrency must not be null");
    }

    public String id() {
        return id;
    }

    /** The currency this account is billed in. */
    public Currency billingCurrency() {
        return billingCurrency;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Account other)) return false;
        return Objects.equals(id, other.id) && billingCurrency == other.billingCurrency;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, billingCurrency);
    }

    @Override
    public String toString() {
        return "Account[id=" + id + ", billingCurrency=" + billingCurrency + "]";
    }
}
