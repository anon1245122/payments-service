# payments-service

Core payment library for ACME. Charges orders through a `PaymentGateway`.

Published as `com.acme:payments-service` and consumed by:

- **this repo** — `checkout`, `web`, `jobs`, `subscriptions`, and `admin` modules;
- **`billing-gateway`** — a *separate* service that depends on the released jar.
  Its callers are **not visible in this repository**, so repo-wide code search will
  not find them.

## Layout

| Package | Role |
|---------|------|
| `com.acme.payments` | the library: `PaymentService`, `Order`, `Account`, `Currency`, `Receipt`, `PaymentGateway` |
| `com.acme.checkout` | in-repo caller — live checkout (highest traffic) |
| `com.acme.web` | in-repo caller — HTTP endpoint |
| `com.acme.jobs` | in-repo caller — nightly retry batch |
| `com.acme.subscriptions` | in-repo caller — recurring renewals |
| `com.acme.admin` | in-repo caller — internal support tool |

## Build

```bash
mvn compile   # main sources only; no network needed
mvn test      # requires JUnit 5 (fetched by Maven)
```

## Active migration

See [`docs/PLAT-1842.md`](docs/PLAT-1842.md): `PaymentService.charge(Order)` is
gaining a required `Currency` parameter. Do not change the signature without the
migration plan — the method is public API with an external consumer.
