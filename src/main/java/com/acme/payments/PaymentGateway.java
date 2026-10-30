package com.acme.payments;

/** Low-level gateway that actually moves money. */
public interface PaymentGateway {
    Receipt submit(String orderId, long amountMinor, Currency currency);
}
