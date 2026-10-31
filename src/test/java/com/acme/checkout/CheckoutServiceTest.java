package com.acme.checkout;

import com.acme.payments.Account;
import com.acme.payments.Currency;
import com.acme.payments.FakeGateway;
import com.acme.payments.Order;
import com.acme.payments.PaymentService;
import com.acme.payments.Receipt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CheckoutServiceTest {

    private PaymentService payments;
    private CheckoutService checkout;

    @BeforeEach
    void setUp() {
        payments = new PaymentService(new FakeGateway());
        checkout = new CheckoutService(payments);
    }

    @Test
    void checkoutCharges() {
        CheckoutService checkout = new CheckoutService(new PaymentService(new FakeGateway()));
        // EUR account — today the receipt silently comes back in USD (the PLAT-1842 bug).
        Order order = new Order("ord-2", new Account("acct-2", Currency.EUR), 5000);

        Receipt receipt = checkout.checkout(order);

        assertEquals("ord-2", receipt.orderId());
    }

    @Test
    void cartValidationAndDiscounts() {
        Order order = new Order("ord-cart", new Account("acct-cart", Currency.USD), 10000);

        assertTrue(checkout.validateCart(order));
        assertEquals(9000, checkout.applyPromotions(order, "DISCOUNT10"));
        assertEquals(8000, checkout.applyPromotions(order, "SAVE20"));
        assertEquals(10000, checkout.applyPromotions(order, "UNKNOWN"));
    }

    @Test
    void cartItemSubtotalAndValidation() {
        CartItem item1 = new CartItem("SKU-1", "Coffee Beans", 1500L, 2);
        CartItem item2 = new CartItem("SKU-2", "Ceramic Mug", 2000L, 1);

        List<CartItem> items = List.of(item1, item2);
        assertTrue(checkout.validateCartItems(items));
        assertEquals(5000L, checkout.calculateCartSubtotal(items));
    }

    @Test
    void inventoryStockReservationAndCommit() {
        checkout.replenishStock("SKU-LAPTOP", 5);
        CartItem item = new CartItem("SKU-LAPTOP", "Laptop", 120000L, 2);

        assertTrue(checkout.reserveCartItems("ord-res-1", List.of(item)));
        assertEquals(3, checkout.getStockLevel("SKU-LAPTOP"));

        checkout.commitInventory("ord-res-1");
        assertEquals(3, checkout.getStockLevel("SKU-LAPTOP"));
    }

    @Test
    void inventoryReleaseOnFailure() {
        checkout.replenishStock("SKU-PHONE", 10);
        CartItem item = new CartItem("SKU-PHONE", "Phone", 80000L, 4);

        assertTrue(checkout.reserveCartItems("ord-cancel", List.of(item)));
        assertEquals(6, checkout.getStockLevel("SKU-PHONE"));

        checkout.releaseInventory("ord-cancel");
        assertEquals(10, checkout.getStockLevel("SKU-PHONE"));
    }

    @Test
    void shippingCalculationsAndThresholds() {
        // Under $75.00: standard shipping applies
        assertEquals(499L, checkout.calculateShippingFee(5000L, ShippingMethod.STANDARD));
        // Over $75.00: free standard ground shipping
        assertEquals(0L, checkout.calculateShippingFee(8000L, ShippingMethod.STANDARD));
        // Express is always billed
        assertEquals(999L, checkout.calculateShippingFee(8000L, ShippingMethod.EXPRESS));
    }

    @Test
    void salesTaxCalculations() {
        long taxableAmount = 10000L; // $100.00
        assertEquals(825L, checkout.calculateTax(taxableAmount, "CA")); // 8.25%
        assertEquals(888L, checkout.calculateTax(taxableAmount, "NY")); // 8.875% rounded
        assertEquals(625L, checkout.calculateTax(taxableAmount, "TX")); // 6.25%
        assertEquals(500L, checkout.calculateTax(taxableAmount, "UNKNOWN")); // 5.0% default
    }

    @Test
    void metricsTracking() {
        Order order = new Order("ord-metric", new Account("acct-m", Currency.USD), 3500);
        checkout.checkout(order);

        assertEquals(1, checkout.getSuccessfulCheckoutCount());
        assertEquals(0, checkout.getFailedCheckoutCount());
        assertEquals(3500, checkout.getTotalRevenueMinor());
        assertEquals(1.0, checkout.getConversionRate());
    }
}
