package com.acme.payments;

import java.time.Clock;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Core payment processing engine for ACME commerce platforms.
 *
 * <p>Coordinates payment lifecycles across merchant channels, including pre-authorizations,
 * two-step captures, direct settlements, refunds, fee calculations, and transaction ledger
 * queries. Enforces transaction constraints, currency validation, and gateway communication.
 *
 * <p>MIGRATION (PLAT-1842): {@link #charge(Order)} currently assumes every charge
 * settles in {@link Currency#USD}. The business is going multi-currency, so this
 * method must take an explicit {@link Currency}. See {@code docs/PLAT-1842.md}
 * for the proposed signature change. Do not change the signature here without a
 * migration plan — this type is published API, consumed in-repo and by the
 * external {@code billing-gateway} service.
 */
public final class PaymentService {

    private static final Logger LOGGER = Logger.getLogger(PaymentService.class.getName());

    public static final long MIN_TRANSACTION_AMOUNT_MINOR = 50L; // $0.50 minimum
    public static final long MAX_TRANSACTION_AMOUNT_MINOR = 10_000_000L; // $100,000 maximum single charge
    public static final long HIGH_RISK_THRESHOLD_MINOR = 500_000L; // $5,000 threshold for enhanced risk evaluation

    private static final Set<Currency> SUPPORTED_SETTLEMENT_CURRENCIES =
            Collections.unmodifiableSet(EnumSet.of(Currency.USD, Currency.EUR, Currency.GBP, Currency.JPY));

    private final PaymentGateway gateway;
    private final Clock clock;
    private final AtomicBoolean operational = new AtomicBoolean(true);
    private final AtomicLong totalProcessedVolumeMinor = new AtomicLong(0);
    private final AtomicLong totalTransactionsCount = new AtomicLong(0);

    private final Map<String, Receipt> settledReceipts = new ConcurrentHashMap<>();
    private final Map<String, Receipt> activeAuthorizations = new ConcurrentHashMap<>();
    private final Map<String, Long> refundedAmounts = new ConcurrentHashMap<>();

    public PaymentService(PaymentGateway gateway) {
        this(gateway, Clock.systemUTC());
    }

    public PaymentService(PaymentGateway gateway, Clock clock) {
        this.gateway = Objects.requireNonNull(gateway, "gateway must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    // -------------------------------------------------------------------------
    // Service Diagnostics & Operational Status
    // -------------------------------------------------------------------------

    /**
     * Diagnostic ping to ensure downstream gateway connectivity.
     */
    public boolean checkHealth() {
        if (!operational.get()) {
            return false;
        }
        try {
            return gateway.ping();
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Gateway ping failed during health check", ex);
            return false;
        }
    }

    /**
     * Checks whether the payment processing pipeline is active.
     */
    public boolean isOperatingNormally() {
        return operational.get();
    }

    /**
     * Administrative switch to pause or resume processing.
     */
    public void setOperationalStatus(boolean active) {
        this.operational.set(active);
        LOGGER.info("Payment service operational status changed to: " + active);
    }

    /**
     * Returns the set of currencies supported for merchant settlement.
     */
    public Set<Currency> getSupportedCurrencies() {
        return SUPPORTED_SETTLEMENT_CURRENCIES;
    }

    /**
     * Checks if a currency is supported for gateway settlement.
     */
    public boolean isCurrencySupported(Currency currency) {
        return currency != null && SUPPORTED_SETTLEMENT_CURRENCIES.contains(currency);
    }

    // -------------------------------------------------------------------------
    // Order Validation & Risk Assessment
    // -------------------------------------------------------------------------

    /**
     * Validates that an order meets formatting, amount bounds, and account constraints.
     */
    public void validateOrder(Order order) {
        Objects.requireNonNull(order, "order must not be null");
        if (order.id() == null || order.id().trim().isEmpty()) {
            throw new IllegalArgumentException("Order ID cannot be null or empty");
        }
        if (order.account() == null) {
            throw new IllegalArgumentException("Order account cannot be null");
        }
        if (order.account().id() == null || order.account().id().trim().isEmpty()) {
            throw new IllegalArgumentException("Account ID cannot be null or empty");
        }
        if (order.amountMinor() < MIN_TRANSACTION_AMOUNT_MINOR) {
            throw new IllegalArgumentException(
                    "Order amount " + order.amountMinor() + " is below minimum allowed " + MIN_TRANSACTION_AMOUNT_MINOR);
        }
        if (order.amountMinor() > MAX_TRANSACTION_AMOUNT_MINOR) {
            throw new IllegalArgumentException(
                    "Order amount " + order.amountMinor() + " exceeds maximum allowed " + MAX_TRANSACTION_AMOUNT_MINOR);
        }
    }

    /**
     * Evaluates an indicative risk rating (0-100) based on order size and account characteristics.
     */
    public int assessRiskScore(Order order) {
        validateOrder(order);
        int score = 10;
        if (order.amountMinor() >= HIGH_RISK_THRESHOLD_MINOR) {
            score += 40;
        }
        if (order.account().billingCurrency() != Currency.USD) {
            score += 15;
        }
        return Math.min(score, 100);
    }

    /**
     * Verifies that customer account credentials are valid and ready for billing.
     */
    public boolean verifyAccount(Account account) {
        if (account == null || account.id() == null || account.id().trim().isEmpty()) {
            return false;
        }
        return isCurrencySupported(account.billingCurrency());
    }

    // -------------------------------------------------------------------------
    // Two-Step Authorization & Capture Pipeline
    // -------------------------------------------------------------------------

    /**
     * Pre-authorizes funds on customer account without immediate settlement.
     */
    public Receipt authorize(Order order) {
        ensureOperational();
        validateOrder(order);

        Receipt auth = gateway.authorize(order.id(), order.amountMinor(), order.account().billingCurrency());
        activeAuthorizations.put(auth.authCode(), auth);
        LOGGER.info("Authorized funds for order " + order.id() + ", authCode=" + auth.authCode());
        return auth;
    }

    /**
     * Captures previously authorized funds for settlement.
     */
    public Receipt capture(String authCode, long amountMinor) {
        ensureOperational();
        Objects.requireNonNull(authCode, "authCode must not be null");

        Receipt auth = activeAuthorizations.get(authCode);
        if (auth == null) {
            throw new IllegalArgumentException("Authorization not found or already captured: " + authCode);
        }
        if (amountMinor <= 0 || amountMinor > auth.amountMinor()) {
            throw new IllegalArgumentException("Invalid capture amount " + amountMinor +
                    "; authorized: " + auth.amountMinor());
        }

        Receipt settlement = gateway.capture(authCode, amountMinor);
        activeAuthorizations.remove(authCode);
        recordSettlement(settlement);
        LOGGER.info("Captured authorization " + authCode + " for order " + settlement.orderId());
        return settlement;
    }

    /**
     * Voids an active authorization before capture.
     */
    public boolean voidAuthorization(String authCode) {
        Objects.requireNonNull(authCode, "authCode must not be null");
        Receipt auth = activeAuthorizations.remove(authCode);
        if (auth == null) {
            return false;
        }
        boolean voided = gateway.voidAuthorization(authCode);
        LOGGER.info("Voided authorization " + authCode + ": " + voided);
        return voided;
    }

    // -------------------------------------------------------------------------
    // Direct Settlement Processing
    // -------------------------------------------------------------------------

    /**
     * Charge an order through the gateway.
     *
     * <p>NOTE: currency is hard-coded to USD. For EUR/GBP/JPY accounts this
     * silently settles in the wrong currency — the reason for PLAT-1842.
     */
    public Receipt charge(Order order) {
        Currency currency = Currency.USD; // implicit assumption PLAT-1842 removes
        Receipt receipt = gateway.submit(order.id(), order.amountMinor(), currency);
        recordSettlement(receipt);
        return receipt;
    }

    // -------------------------------------------------------------------------
    // Refunds & Reversals
    // -------------------------------------------------------------------------

    /**
     * Issues a full or partial refund for a settled transaction.
     */
    public Receipt refund(String orderId, long amountMinor, String reason) {
        ensureOperational();
        Objects.requireNonNull(orderId, "orderId must not be null");
        Objects.requireNonNull(reason, "reason must not be null");

        Receipt original = settledReceipts.get(orderId);
        if (original == null) {
            throw new IllegalArgumentException("No settled transaction found for order ID: " + orderId);
        }

        long alreadyRefunded = refundedAmounts.getOrDefault(orderId, 0L);
        long remaining = original.amountMinor() - alreadyRefunded;
        if (amountMinor <= 0 || amountMinor > remaining) {
            throw new IllegalArgumentException("Refund amount " + amountMinor +
                    " exceeds refundable balance of " + remaining);
        }

        Receipt refundReceipt = gateway.refund(orderId, amountMinor, reason);
        refundedAmounts.put(orderId, alreadyRefunded + amountMinor);
        LOGGER.info("Processed refund of " + amountMinor + " for order " + orderId + "; reason: " + reason);
        return refundReceipt;
    }

    /**
     * Computes remaining balance eligible for refund on a settled order.
     */
    public long calculateRefundableAmount(String orderId) {
        Receipt original = settledReceipts.get(orderId);
        if (original == null) {
            return 0L;
        }
        return original.amountMinor() - refundedAmounts.getOrDefault(orderId, 0L);
    }

    // -------------------------------------------------------------------------
    // Fee Estimation & Foreign Exchange Rates
    // -------------------------------------------------------------------------

    /**
     * Calculates the estimated merchant processing fee in minor units.
     * Default interchange schedule: 2.9% + 30 cents, plus 1.0% international cross-border fee.
     */
    public long calculateProcessingFee(long amountMinor, boolean isInternational) {
        if (amountMinor <= 0) {
            return 0L;
        }
        double percentage = isInternational ? 0.039 : 0.029;
        long fixedFee = 30L;
        long calculated = Math.round(amountMinor * percentage) + fixedFee;
        return Math.min(calculated, amountMinor);
    }

    /**
     * Provides an indicative exchange rate multiplier between supported currencies.
     */
    public double estimateExchangeRate(Currency source, Currency target) {
        if (source == target) {
            return 1.0;
        }
        if (source == Currency.USD && target == Currency.EUR) return 0.92;
        if (source == Currency.EUR && target == Currency.USD) return 1.087;
        if (source == Currency.USD && target == Currency.GBP) return 0.79;
        if (source == Currency.GBP && target == Currency.USD) return 1.265;
        if (source == Currency.USD && target == Currency.JPY) return 155.0;
        if (source == Currency.JPY && target == Currency.USD) return 0.00645;
        return 1.0;
    }

    // -------------------------------------------------------------------------
    // Ledger Inquiries & Operational Metrics
    // -------------------------------------------------------------------------

    /**
     * Looks up a settled receipt by order ID.
     */
    public Optional<Receipt> findReceipt(String orderId) {
        return Optional.ofNullable(settledReceipts.get(orderId));
    }

    /**
     * Looks up an active pre-authorization by authorization code.
     */
    public Optional<Receipt> findActiveAuthorization(String authCode) {
        return Optional.ofNullable(activeAuthorizations.get(authCode));
    }

    /**
     * Returns total number of completed settlements and captures.
     */
    public long getTotalTransactionsCount() {
        return totalTransactionsCount.get();
    }

    /**
     * Returns cumulative volume settled across all successful charges.
     */
    public long getTotalProcessedVolumeMinor() {
        return totalProcessedVolumeMinor.get();
    }

    /**
     * Resets in-memory ledger state (primarily for test fixture cleanup).
     */
    public void clearLedger() {
        settledReceipts.clear();
        activeAuthorizations.clear();
        refundedAmounts.clear();
        totalProcessedVolumeMinor.set(0);
        totalTransactionsCount.set(0);
    }

    // -------------------------------------------------------------------------
    // Internal Helper Methods
    // -------------------------------------------------------------------------

    private void ensureOperational() {
        if (!operational.get()) {
            throw new IllegalStateException("Payment service is currently paused for maintenance");
        }
    }

    private void recordSettlement(Receipt receipt) {
        if (receipt != null) {
            settledReceipts.put(receipt.orderId(), receipt);
            totalProcessedVolumeMinor.addAndGet(receipt.amountMinor());
            totalTransactionsCount.incrementAndGet();
        }
    }
}
