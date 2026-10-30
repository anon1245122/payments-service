package com.acme.subscriptions;

import com.acme.payments.Account;
import com.acme.payments.Currency;
import com.acme.payments.Order;

import java.util.Objects;

/** A recurring subscription plan. */
public final class Subscription {

    private final String id;
    private final Account account;
    private final long priceMinor;
    private final Currency planCurrency;

    public Subscription(String id, Account account, long priceMinor, Currency planCurrency) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.account = Objects.requireNonNull(account, "account must not be null");
        this.priceMinor = priceMinor;
        this.planCurrency = Objects.requireNonNull(planCurrency, "planCurrency must not be null");
    }

    public String id() {
        return id;
    }

    public Account account() {
        return account;
    }

    public long priceMinor() {
        return priceMinor;
    }

    /** The currency the plan is priced in — the correct currency for renewals. */
    public Currency planCurrency() {
        return planCurrency;
    }

    public Order toOrder() {
        return new Order("sub-" + id, account, priceMinor);
    }
}
