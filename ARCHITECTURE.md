# DevData Factory — Architecture & Roadmap

## Vision

Transform this app from a single-purpose contact generator into a
**portable developer toolkit** for QA engineers, backend developers,
and security researchers (bug bounty hunters).

The mental model: **Postman for test data** — pick a data type, configure
parameters, generate thousands of records, export in the format your tool needs.

Inspired by web-based utilities like [bankutils.com](https://www.bankutils.com)
— but as a fully offline, native Android app that works in air-gapped environments,
on staging devices, and during live testing sessions.

---

## Current State (v1.x)

```
App
└── Contact Generator (VCF)
    ├── FakeDataGenerator   — names, phones, emails, companies
    ├── VcfGenerator        — writes vCard 3.0 files
    ├── BatchProcessor      — runs multiple jobs sequentially
    ├── TemplateRepository  — save/load generation configs
    └── BillingManager      — free (1k) / premium (10k) tiers
```

---

## Target Architecture (v2.x — DevData Factory)

### Core Layer (`core/`)

```
DataGenerator<T>        ← interface every tool implements
OutputFormat            ← VCF | CSV | JSON | SQL | TXT
GeneratorRegistry       ← plugin registry; UI discovers tools here
```

Adding a new tool = implement `DataGenerator<T>` + call `GeneratorRegistry.register()`.
No other files need to change.

### Generator Modules

```
generators/
├── contacts/
│   └── ContactGenerator    (existing, refactored to DataGenerator<ContactRecord>)
│
├── finance/                ← NEW — bankutils-style financial test data
│   ├── CardGenerator       Luhn-valid Visa/MC/Amex/Mir card numbers + expiry + CVV
│   ├── IbanGenerator       Mod-97 valid IBANs for DE/GB/FR/ES/NL/PL/UA/CH
│   ├── LuhnAlgorithm       pure check-digit logic (also usable as a standalone validator)
│   └── LuhnValidator       UI-facing validator tool ("is this card number valid?")
│
├── identity/               ← PLANNED
│   ├── PassportGenerator   realistic passport numbers (format-valid, not real)
│   ├── SsnGenerator        US SSN / UK NIN / DE Steuer-ID patterns
│   └── AddressGenerator    locale-aware street addresses
│
├── network/                ← PLANNED
│   ├── IPv4Generator       random IPs, CIDR ranges, private/public
│   ├── IPv6Generator
│   ├── MacAddressGenerator OUI-prefix aware
│   └── JwtGenerator        signed HS256 tokens with configurable payload
│
└── web/                    ← PLANNED
    ├── EmailGenerator      standalone (not tied to a contact name)
    ├── UrlGenerator        realistic API endpoint paths
    └── UuidGenerator       v4/v7, bulk, formatted
```

### Output Pipeline

```
DataGenerator<T>
    .generate() → T
    .serialize(T, OutputFormat) → String
    .serializeBatch(List<T>, OutputFormat) → String (with headers/footers)
         ↓
    FileExporter — writes to external storage, returns FileProvider Uri
         ↓
    ShareSheet / Copy to clipboard / ADB pull
```

Supported formats per generator:

| Generator    | CSV | JSON | SQL | VCF | TXT |
|--------------|-----|------|-----|-----|-----|
| Contacts     | ✓   | ✓    | ✓   | ✓   | -   |
| Cards        | ✓   | ✓    | -   | -   | ✓   |
| IBANs        | ✓   | ✓    | ✓   | -   | ✓   |
| Passports    | ✓   | ✓    | -   | -   | ✓   |
| IPv4/IPv6    | ✓   | ✓    | -   | -   | ✓   |
| JWTs         | -   | ✓    | -   | -   | ✓   |

---

## Use Cases by Audience

### QA Engineers
- **Contacts**: seed CRM/phonebook apps, test import flows, stress-test
  contacts sync with 10k entries
- **Cards**: fill payment forms in staging; test field validation (length,
  Luhn, expiry format); generate edge cases (all-zeros CVV, expired dates)
- **IBANs**: test SEPA transfer screens; validate error messages for
  wrong check digits (generator can deliberately produce invalid ones)
- **Addresses**: fill shipping forms; test locale-specific postal code validation

### Backend Developers
- **SQL output**: `INSERT INTO users ...` — drop directly into seed scripts
- **JSON output**: Postman collection mock responses, OpenAPI example bodies
- **CSV output**: import into any database tool (TablePlus, DataGrip, pgAdmin)
- **Batch processing**: generate 50 different files in one tap for load testing

### Bug Bounty Hunters & Security Researchers
- **LuhnValidator**: quickly check if a target accepts non-Luhn card numbers
  (client-side bypass testing)
- **CardGenerator**: generate cards of all schemes to probe BIN validation logic
- **IbanGenerator**: test for IBAN injection, format confusion, country-code handling
- **JwtGenerator** (planned): craft tokens with weak secrets, wrong algorithms,
  missing claims — for JWT security testing
- **Offline operation**: works without internet; safe for air-gapped targets
- **Reproducible seeds**: fixed Random seed → same dataset every run for reproducible reports

---

## Monetization Tiers (proposed)

| Feature                          | Free   | Pro ($4.99) | Team ($9.99/mo) |
|----------------------------------|--------|-------------|-----------------|
| Contacts (max)                   | 1,000  | 10,000      | unlimited       |
| Finance generators               | —      | ✓           | ✓               |
| Identity generators              | —      | —           | ✓               |
| SQL / JSON export                | —      | ✓           | ✓               |
| Batch jobs (max)                 | 3      | 20          | unlimited       |
| Reproducible seeds               | —      | ✓           | ✓               |
| API (HTTP server mode)           | —      | —           | ✓               |

---

## Implementation Plan

### Phase 1 — Architecture foundation (this branch)
- [x] `DataGenerator<T>` interface
- [x] `OutputFormat` sealed class
- [x] `GeneratorRegistry` plugin registry
- [x] `LuhnAlgorithm` + `LuhnValidator`
- [x] `CardGenerator` (Visa/MC/Amex/Discover/UnionPay/Mir)
- [x] `IbanGenerator` (8 countries, Mod-97 valid)

### Phase 2 — Contacts refactor
- [x] Extract `ContactRecord` data class (`generators/contacts`)
- [x] Refactor `FakeDataGenerator` → `ContactGenerator : DataGenerator<ContactRecord>` (registered; appears in the tool picker)
- [x] Add CSV/JSON/SQL serialization to contacts (plus VCF); `VcfGenerator` now delegates to `ContactGenerator`
- [ ] Wire the registry generators into `BatchProcessor` (still contacts/VCF-only)

### Phase 3 — Identity generators
- [x] `SyntheticIdentityGenerator` — full identities incl. passports + ICAO 9303 MRZ (`MrzBuilder`)
- [x] `TaxIdGenerator` — US SSN, UK NIN, DE, ES, UA, PL, FR, IT tax IDs (with validators)
- [x] `AddressGenerator` with locale datasets (`WorldPostalFormats`)

### Phase 4 — Network & Web generators
- [x] `IPv4Generator`, `IPv6Generator`, `MacAddressGenerator`
- [x] `JwtGenerator` (HS256/RS256/none, configurable claims)
- [x] `UuidGenerator` (v4 + v7)

### Phase 5 — UI redesign
- [x] Tool picker screen (registry-driven, grouped generator cards) — `devtools/DevToolsScreen`
- [x] Per-tool configuration screen (count, output format, reproducible seed)
- [x] Unified export screen (format picker + copy/share via `DevToolsExporter`)
- [ ] History screen shows all generator types, not just contacts

---

## Key Design Decisions

**Why `DataGenerator<T>` instead of a simpler function type?**
The interface forces each tool to declare its supported formats and provide
both single and batch serialization. This makes the UI format-picker work
generically without knowing the concrete generator type.

**Why `GeneratorRegistry` instead of a hardcoded list?**
Future generators can be added (or removed for different build flavors) without
touching any UI code. A "lite" build flavor can register only free-tier generators.

**Why LuhnAlgorithm as a separate object?**
It's independently testable, has zero dependencies, and will be reused by
CardGenerator, LuhnValidator, and any future tool that needs check-digit logic
(e.g. IMEI generators use Luhn too).

**Why not use a DI framework (Hilt/Koin)?**
The app is small enough that manual injection through the ViewModel is clear and
avoids annotation processing overhead. `GeneratorRegistry` serves as a lightweight
service locator for the tool discovery use case specifically.
