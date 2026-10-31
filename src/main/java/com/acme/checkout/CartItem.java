package com.acme.checkout;

import java.util.Objects;

/**
 * An individual item line within a customer's shopping cart.
 */
public final class CartItem {

    private final String sku;
    private final String title;
    private final long unitPriceMinor;
    private final int quantity;
    private final boolean taxable;

    public CartItem(String sku, String title, long unitPriceMinor, int quantity) {
        this(sku, title, unitPriceMinor, quantity, true);
    }

    public CartItem(String sku, String title, long unitPriceMinor, int quantity, boolean taxable) {
        this.sku = Objects.requireNonNull(sku, "sku must not be null");
        this.title = Objects.requireNonNull(title, "title must not be null");
        if (unitPriceMinor < 0) {
            throw new IllegalArgumentException("unitPriceMinor cannot be negative: " + unitPriceMinor);
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive: " + quantity);
        }
        this.unitPriceMinor = unitPriceMinor;
        this.quantity = quantity;
        this.taxable = taxable;
    }

    public String sku() {
        return sku;
    }

    public String title() {
        return title;
    }

    public long unitPriceMinor() {
        return unitPriceMinor;
    }

    public int quantity() {
        return quantity;
    }

    public boolean isTaxable() {
        return taxable;
    }

    public long totalLineAmountMinor() {
        return unitPriceMinor * quantity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CartItem other)) return false;
        return unitPriceMinor == other.unitPriceMinor &&
                quantity == other.quantity &&
                taxable == other.taxable &&
                Objects.equals(sku, other.sku) &&
                Objects.equals(title, other.title);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sku, title, unitPriceMinor, quantity, taxable);
    }

    @Override
    public String toString() {
        return "CartItem[sku=" + sku + ", title=" + title + ", qty=" + quantity + ", unitPrice=" + unitPriceMinor + "]";
    }
}
