# GST Invoice Reconciliation Tool

A Spring Boot web application that reconciles a purchase register against GSTR-2B workbook data. It normalizes invoice numbers, matches records one-to-one, classifies amount differences and missing invoices, estimates ITC at risk, stores run history, and exports a multi-sheet Excel report.

## Run locally

Prerequisites: JDK 17 or newer and Maven 3.8+. The default profile uses a local file-backed H2 database, so MySQL is not required to try the application.

```bash
APP_ADMIN_USERNAME=admin APP_ADMIN_PASSWORD='replace-with-a-unique-password-at-least-12-characters' mvn spring-boot:run
```

On the first startup, the app uses these environment variables to create the initial administrator account. Choose a private password of at least 12 characters. Once an account exists in the database, the bootstrap variables are not used to reset it. Open [http://localhost:8081](http://localhost:8081) and sign in. Upload one purchase register and one GSTR-2B `.xlsx` workbook or `.csv` file. For workbooks, the first sheet must have these exact headers in this order; CSV files must have the same header row:

| Column | Example |
| --- | --- |
| GSTIN | `29ABCDE1234F1Z5` |
| Supplier Name | `ABC Traders` |
| Invoice Number | `INV-001` |
| Invoice Date | `15-08-2026` |
| Taxable Value | `100000.00` |
| IGST | `0.00` |
| CGST | `9000.00` |
| SGST | `9000.00` |

The reader uses the first worksheet. It supports `.xlsx` and `.csv` files up to 20 MB each and accepts Excel dates, ISO dates, day-first or month-first slash/hyphen dates, two-digit years, and common Excel serial dates. This project intentionally uses a simplified input template; it does not parse every section and variation in a raw GST portal download.

## Use MySQL

Create the database, then start with the MySQL profile and credentials supplied through environment variables:

```sql
CREATE DATABASE gst_recon CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

```bash
APP_ADMIN_USERNAME=admin APP_ADMIN_PASSWORD='replace-with-a-unique-password-at-least-12-characters' DB_USERNAME=root DB_PASSWORD=your_password mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```

You can also set `DB_URL` and `DB_DRIVER`. Hibernate creates or updates the reconciliation and user tables. Use a managed secret store for credentials outside local development.

## Reconciliation flow

1. Read the first worksheet and parse invoice fields.
2. Validate GSTIN, required fields, and nonnegative amounts; invalid rows are listed and excluded. An unreadable date is reported, but the row remains eligible for exact matching.
3. Normalize GSTIN and invoice number (uppercase, remove punctuation and spaces, strip leading zeroes).
4. Pair exact GSTIN plus normalized invoice keys using a queue, preserving duplicate rows as one-to-one records.
5. For remaining rows, compare invoice numbers only within the same GSTIN; fuzzy candidates must be within seven invoice-date days and meet the configured threshold. Ambiguous best scores are left unmatched.
6. Classify paired records as Matched or Mismatch using a ₹1.00 tolerance for taxable value and total tax.
7. Classify leftovers as Missing in GST or Missing in Books.
8. Calculate ITC at risk as all books tax for Missing in GST plus positive excess books tax on Mismatch rows.
9. Save the run and display the results; export Summary, Matched, Mismatch, Missing in GST, and Missing in Books sheets.

Exact matching is linear in the input size on average with hash lookups. Fuzzy matching is quadratic in the number of unmatched candidates within each GSTIN group, multiplied by invoice-string edit-distance cost. Levenshtein distance uses rolling rows, reducing memory to O(minimum string length).

## Configuration

| Setting | Default | Environment override |
| --- | --- | --- |
| Fuzzy score threshold | `0.85` | `RECON_FUZZY_THRESHOLD` |
| Per-file upload limit | `20MB` | Configure in `application.properties` |
| Multipart request limit | `40MB` | Configure in `application.properties` |
| Database | file-backed H2 | `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `DB_DRIVER` |
| Web port | `8081` | `SERVER_PORT` |

## Dashboard and account roles

Every page requires sign-in. Passwords are stored as BCrypt hashes. The initial admin account is created only when the user table is empty and `APP_ADMIN_PASSWORD` is configured. Admins can create additional accounts from **Users**.

If you need to reset the initial admin password, stop the app and restart once with `APP_ADMIN_USERNAME=admin APP_ADMIN_PASSWORD='your-new-private-password' APP_ADMIN_RESET_PASSWORD=true mvn spring-boot:run`. The reset flag must be explicitly enabled and the named account must already have the admin role. Omit the flag during normal restarts.

| Role | Access |
| --- | --- |
| Accountant | Upload and reconcile files; view and download only runs created by that username |
| Manager | View and download all saved reconciliation runs; read-only |
| Admin | Full access, including reconciliation and account creation |

The dashboard’s headline metrics describe the latest run visible to the signed-in user. Its Chart.js trend chart compares status counts across the 10 latest visible runs. Chart.js loads from jsDelivr, so the chart requires internet access in the browser.

## ITC and matching limitations

- The displayed ITC risk uses the difference in total IGST + CGST + SGST from books and portal data. It is a reconciliation estimate, not tax advice or a filing decision.
- The ₹1.00 tolerance applies separately to taxable value and total tax. Per-tax-component variance is not classified separately.
- Fuzzy invoice matching is limited to a seven-day date window and uses invoice-string similarity. Rows without a valid date can still exact-match, but cannot fuzzy-match. Tune `RECON_FUZZY_THRESHOLD` against a labeled dataset before operational use; no accuracy or threshold-tuning figures are claimed here because no labeled files were supplied.
- Invalid rows are skipped with row-level issues. Rows with unparsable numeric values cause the upload to fail with a row reference.
- Credit/debit note semantics, supplier-name fallback, one-to-many matching, raw portal formats, and large-file performance have not been implemented or measured.
- This starter app uses username-based roles and scopes accountant run history by username. For deployment, add organization/tenant boundaries, HTTPS, account recovery, audit logging, and operational credential management.

## Project structure

`src/main/java/com/example/gstrecon` contains the Spring Boot application, matching and validation services, Excel reader and report writer, JPA history and user entities, security configuration, and normalization utilities. Thymeleaf views and CSS are in `src/main/resources/templates` and `src/main/resources/static`.
