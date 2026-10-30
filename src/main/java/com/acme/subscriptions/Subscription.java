package com.acme.subscriptions;

import com.acme.payments.Account;
import com.acme.payments.Currency;
import com.acme.payments.Order;

/** A recurring subscription plan. */
public final class Subscription {

    private final String id;
    private final Account account;
    private final long priceMinor;
    private final Currency planCurrency;

    public Subscription(String id, Account account, long priceMinor, Currency planCurrency) {
        this.id = id;
        this.account = account;
        this.priceMinor = priceMinor;
        this.planCurrency = planCurrency;
    }

    /** The currency the plan is priced in — the correct currency for renewals. */
    public Currency planCurrency() {
        return planCurrency;
    }

    public Order toOrder() {
        return new Order("sub-" + id, account, priceMinor);
    }
}
