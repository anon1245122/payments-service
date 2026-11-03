# PLAT-1842 Migration Plan
charge(Order) gains currency parameter

## Customer Migration Patterns
- Out of box: 3 month upgrade
- Onsite Integrators: 12 months
- Third Party Integrators: 36 months
- Off network: 40 months
Key Takeaway: deprecated methods might need to stay in the product for 3-4 years.

## Code Survey
- Billing Gateway uses payment service as a library. Billing gateway is external.
- PaymentController: account preference or a value from API is possible but unresolved as which is correct.
- RetryFailedPaymentsJob: Must use order's original currency. Cant use accounts.
- SubscriptionRenewer: Subscription carries planCurrency.
- PaymentService: charge overload after first chart function declaration
- CheckService: order carries account billing. Fallback to account default if needed.
- ManualChargeTool: Pull currency from order itself.

## Implementation Strategies
- Admin warning for the deprecation
- Documentation on the deprecated method (or extend existing)
- Add an overloaded charge method.
- Adding a default currency that can be changed globally
- Mark old charge method as deprecated
- Consider attaching currency to an Order.
- Remove old charge method.

## Release Plan
- Newsletter
  - Admin warning
  - Timeline from above
  - Feedback
- Release 14.5
  - Admin warning and documentation
  - Add overloaded charge method
  - Allow settings to configure default currecy (if not configured, opt out)
  - Mark old charge method as deprecated.
  - Add currency to the Order object
- Release major X.0
  - X.0 because 3-4 years is long
  - 2-3 years have review. New newsletter
  - Remove the old charge method.
