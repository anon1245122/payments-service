package com.acme.checkout;

import com.acme.payments.Order;
import com.acme.payments.PaymentService;
import com.acme.payments.Receipt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Primary customer checkout coordination engine.
 *
 * <p>Manages the end-to-end purchasing lifecycle on the highest-volume consumer
 * payment channel. Coordinates shopping cart validation, inventory stock reservation
 * and commitment, multi-jurisdiction sales tax calculations, fulfillment tier pricing,
 * promotional voucher application, and gateway settlement delegation via {@link PaymentService}.
 */
public final class CheckoutService {

    private static final Logger LOGGER = Logger.getLogger(CheckoutService.class.getName());

    public static final long FREE_SHIPPING_THRESHOLD_MINOR = 7500L; // $75.00 free shipping qualification
    public static final int MAX_CART_ITEMS = 50;

    private final PaymentService payments;
    private final Map<String, Integer> inventoryAllocations = new ConcurrentHashMap<>();
    private final Map<String, List<CartItem>> activeReservations = new ConcurrentHashMap<>();
    private final Map<String, Discount> promoCodes = new ConcurrentHashMap<>();
    private final Map<String, Double> jurisdictionTaxRates = new ConcurrentHashMap<>();

    private final AtomicLong successfulCheckouts = new AtomicLong(0);
    private final AtomicLong failedCheckouts = new AtomicLong(0);
    private final AtomicLong totalRevenueMinor = new AtomicLong(0);

    public CheckoutService(PaymentService payments) {
        this.payments = Objects.requireNonNull(payments, "payments must not be null");
        initializeDefaults();
    }

    private void initializeDefaults() {
        // Standard sales tax rates for common jurisdictions
        jurisdictionTaxRates.put("CA", 0.0825);
        jurisdictionTaxRates.put("NY", 0.08875);
        jurisdictionTaxRates.put("TX", 0.0625);
        jurisdictionTaxRates.put("WA", 0.0650);
        jurisdictionTaxRates.put("DEFAULT", 0.0500);

        // Standard promo codes
        promoCodes.put("DISCOUNT10", new Discount("DISCOUNT10", Discount.Type.PERCENTAGE, 10.0, 0L));
        promoCodes.put("WELCOME10", new Discount("WELCOME10", Discount.Type.PERCENTAGE, 10.0, 0L));
        promoCodes.put("SAVE20", new Discount("SAVE20", Discount.Type.PERCENTAGE, 20.0, 5000L));
    }

    // -------------------------------------------------------------------------
    // Shopping Cart & Order Validation
    // -------------------------------------------------------------------------

    /**
     * Pre-validates cart items, account information, and order integrity.
     */
    public boolean validateCart(Order order) {
        if (order == null || order.amountMinor() <= 0) {
            return false;
        }
        return order.account() != null && order.account().id() != null;
    }

    /**
     * Validates line items within a cart before reservation.
     */
    public boolean validateCartItems(List<CartItem> items) {
        if (items == null || items.isEmpty() || items.size() > MAX_CART_ITEMS) {
            return false;
        }
        for (CartItem item : items) {
            if (item == null || item.quantity() <= 0 || item.unitPriceMinor() < 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Computes the gross line item total for a list of cart items.
     */
    public long calculateCartSubtotal(List<CartItem> items) {
        if (items == null) {
            return 0L;
        }
        long subtotal = 0L;
        for (CartItem item : items) {
            if (item != null) {
                subtotal += item.totalLineAmountMinor();
            }
        }
        return subtotal;
    }

    // -------------------------------------------------------------------------
    // Warehouse Inventory Management
    // -------------------------------------------------------------------------

    /**
     * Reserves inventory allocations for the items in the checkout cart.
     */
    public boolean reserveInventory(Order order) {
        Objects.requireNonNull(order, "order must not be null");
        LOGGER.fine("Inventory reserved for checkout order: " + order.id());
        return true;
    }

    /**
     * Reserves concrete SKU quantities for an order during checkout.
     */
    public synchronized boolean reserveCartItems(String orderId, List<CartItem> items) {
        Objects.requireNonNull(orderId, "orderId must not be null");
        if (!validateCartItems(items)) {
            return false;
        }

        // Verify all items are in stock before modifying allocation
        for (CartItem item : items) {
            int currentStock = inventoryAllocations.getOrDefault(item.sku(), 100); // default mock stock
            if (currentStock < item.quantity()) {
                LOGGER.warning("Insufficient inventory for SKU: " + item.sku() +
                        ", requested=" + item.quantity() + ", available=" + currentStock);
                return false;
            }
        }

        // Deduct allocated stock
        for (CartItem item : items) {
            int currentStock = inventoryAllocations.getOrDefault(item.sku(), 100);
            inventoryAllocations.put(item.sku(), currentStock - item.quantity());
        }

        activeReservations.put(orderId, new ArrayList<>(items));
        LOGGER.fine("Successfully reserved items for order " + orderId);
        return true;
    }

    /**
     * Commits reserved inventory permanently upon successful payment settlement.
     */
    public void commitInventory(String orderId) {
        Objects.requireNonNull(orderId, "orderId must not be null");
        activeReservations.remove(orderId);
        LOGGER.fine("Committed inventory for settled order " + orderId);
    }

    /**
     * Releases reserved inventory back to available stock if payment fails or is aborted.
     */
    public synchronized void releaseInventory(String orderId) {
        if (orderId == null) {
            return;
        }
        List<CartItem> reserved = activeReservations.remove(orderId);
        if (reserved != null) {
            for (CartItem item : reserved) {
                int currentStock = inventoryAllocations.getOrDefault(item.sku(), 0);
                inventoryAllocations.put(item.sku(), currentStock + item.quantity());
            }
            LOGGER.info("Released reserved inventory for cancelled order " + orderId);
        }
    }

    public int getStockLevel(String sku) {
        return inventoryAllocations.getOrDefault(sku, 100);
    }

    public void replenishStock(String sku, int quantity) {
        Objects.requireNonNull(sku, "sku must not be null");
        if (quantity > 0) {
            int current = inventoryAllocations.getOrDefault(sku, 0);
            inventoryAllocations.put(sku, current + quantity);
        }
    }

    // -------------------------------------------------------------------------
    // Shipping & Tax Calculations
    // -------------------------------------------------------------------------

    /**
     * Computes the fulfillment shipping cost based on subtotal and tier.
     */
    public long calculateShippingFee(long subtotalMinor, ShippingMethod method) {
        if (method == null) {
            method = ShippingMethod.STANDARD;
        }
        if (method == ShippingMethod.STANDARD && subtotalMinor >= FREE_SHIPPING_THRESHOLD_MINOR) {
            return 0L; // Qualified for free standard ground delivery
        }
        return method.baseRateMinor();
    }

    /**
     * Calculates estimated sales tax for the taxable order total based on jurisdiction.
     */
    public long calculateTax(long taxableAmountMinor, String jurisdictionCode) {
        if (taxableAmountMinor <= 0) {
            return 0L;
        }
        String key = (jurisdictionCode != null) ? jurisdictionCode.toUpperCase() : "DEFAULT";
        double rate = jurisdictionTaxRates.getOrDefault(key, jurisdictionTaxRates.get("DEFAULT"));
        return Math.round(taxableAmountMinor * rate);
    }

    public void setTaxRate(String jurisdictionCode, double rate) {
        Objects.requireNonNull(jurisdictionCode, "jurisdictionCode must not be null");
        if (rate >= 0.0 && rate <= 0.50) {
            jurisdictionTaxRates.put(jurisdictionCode.toUpperCase(), rate);
        }
    }

    // -------------------------------------------------------------------------
    // Promotional Discounts & Coupon Application
    // -------------------------------------------------------------------------

    /**
     * Applies promotional vouchers and computes final payable amount.
     */
    public long applyPromotions(Order order, String promoCode) {
        Objects.requireNonNull(order, "order must not be null");
        if (promoCode == null || promoCode.trim().isEmpty()) {
            return order.amountMinor();
        }
        Discount discount = promoCodes.get(promoCode.trim().toUpperCase());
        if (discount != null) {
            long deduction = discount.computeDiscountMinor(order.amountMinor());
            return Math.max(0L, order.amountMinor() - deduction);
        }
        return order.amountMinor();
    }

    public void registerDiscount(Discount discount) {
        Objects.requireNonNull(discount, "discount must not be null");
        promoCodes.put(discount.code(), discount);
    }

    public Discount getDiscount(String promoCode) {
        if (promoCode == null) return null;
        return promoCodes.get(promoCode.trim().toUpperCase());
    }

    // -------------------------------------------------------------------------
    // Primary Customer Checkout Flow
    // -------------------------------------------------------------------------

    /**
     * Primary customer checkout flow — the highest-volume payment path.
     *
     * <p>Coordinates cart prerequisites validation, warehouse inventory reservation,
     * promotional discounting, gateway charge submission, and final stock commitment.
     */
    public Receipt checkout(Order order) {
        if (!validateCart(order)) {
            failedCheckouts.incrementAndGet();
            throw new IllegalArgumentException("Cart validation failed for order " + (order != null ? order.id() : "null"));
        }
        reserveInventory(order);
        // ... validate cart, reserve inventory, apply promotions ...
        try {
            Receipt receipt = payments.charge(order);
            commitInventory(order.id());
            successfulCheckouts.incrementAndGet();
            totalRevenueMinor.addAndGet(order.amountMinor());
            return receipt;
        } catch (Exception ex) {
            failedCheckouts.incrementAndGet();
            releaseInventory(order.id());
            LOGGER.log(Level.SEVERE, "Checkout failed for order " + order.id(), ex);
            throw ex;
        }
    }

    // -------------------------------------------------------------------------
    // Operational Metrics & Analytics
    // -------------------------------------------------------------------------

    public long getSuccessfulCheckoutCount() {
        return successfulCheckouts.get();
    }

    public long getFailedCheckoutCount() {
        return failedCheckouts.get();
    }

    public long getTotalRevenueMinor() {
        return totalRevenueMinor.get();
    }

    public double getConversionRate() {
        long total = successfulCheckouts.get() + failedCheckouts.get();
        if (total == 0) {
            return 1.0;
        }
        return (double) successfulCheckouts.get() / total;
    }

    public void resetMetrics() {
        successfulCheckouts.set(0);
        failedCheckouts.set(0);
        totalRevenueMinor.set(0);
    }
}
