package com.acme.payments;

/** In-memory gateway used in tests and local runs. */
public final class FakeGateway implements PaymentGateway {

    @Override
    public Receipt submit(String orderId, long amountMinor, Currency currency) {
        return new Receipt(orderId, amountMinor, currency, "AUTH-" + orderId);
    }
}
