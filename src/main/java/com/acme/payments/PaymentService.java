package com.acme.payments;

/**
 * Entry point for charging orders.
 *
 * <p>MIGRATION (PLAT-1842): {@link #charge(Order)} currently assumes every charge
 * settles in {@link Currency#USD}. The business is going multi-currency, so this
 * method must take an explicit {@link Currency}. See {@code docs/PLAT-1842.md}
 * for the proposed signature change. Do not change the signature here without a
 * migration plan — this type is published API, consumed in-repo and by the
 * external {@code billing-gateway} service.
 */
public final class PaymentService {

    private final PaymentGateway gateway;

    public PaymentService(PaymentGateway gateway) {
        this.gateway = gateway;
    }

    /**
     * Charge an order through the gateway.
     *
     * <p>NOTE: currency is hard-coded to USD. For EUR/GBP/JPY accounts this
     * silently settles in the wrong currency — the reason for PLAT-1842.
     */
    public Receipt charge(Order order) {
        Currency currency = Currency.USD; // implicit assumption PLAT-1842 removes
        return gateway.submit(order.id(), order.amountMinor(), currency);
    }
}
