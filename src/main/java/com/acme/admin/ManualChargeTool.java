package com.acme.admin;

import com.acme.payments.Order;
import com.acme.payments.PaymentService;
import com.acme.payments.Receipt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

/** Internal support tool for manually charging an order. Low traffic. */
public final class ManualChargeTool {

    private static final Logger LOGGER = Logger.getLogger(ManualChargeTool.class.getName());

    private final PaymentService payments;
    private final List<String> auditTrail = Collections.synchronizedList(new ArrayList<>());

    public ManualChargeTool(PaymentService payments) {
        this.payments = Objects.requireNonNull(payments, "payments must not be null");
    }

    public List<String> getAuditTrail() {
        return Collections.unmodifiableList(new ArrayList<>(auditTrail));
    }

    public Receipt forceCharge(Order order) {
        return forceCharge(order, "support-override", "ADMIN");
    }

    public Receipt forceCharge(Order order, String reason, String operatorId) {
        Objects.requireNonNull(order, "order must not be null");
        Objects.requireNonNull(reason, "reason must not be null");
        Objects.requireNonNull(operatorId, "operatorId must not be null");

        String auditEntry = String.format("OPERATOR=%s REASON=%s ORDER=%s AMOUNT=%d",
                operatorId, reason, order.id(), order.amountMinor());
        auditTrail.add(auditEntry);
        LOGGER.warning("MANUAL CHARGE INITIATED: " + auditEntry);

        return payments.charge(order);
    }
}
