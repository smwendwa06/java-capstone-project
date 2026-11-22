# M-Kopo SACCO Management System

A console-based Java application built to manage SACCO member registrations, savings and loan accounts, PIN-authenticated transactions and persistent audit logs.

## Features
- **Object-Oriented Architecture**: Encapsulated domain models (`Member`, `Transaction`), abstract `Account` base class with polymorphic `calculateInterest()` overrides in `SavingsAccount` (5%) and `LoanAccount` (12%), `Auditable` interface, and custom `InsufficientFundsException`.
- **Collections & Persistence**: `HashMap` for member lookups, `HashSet` for unique National ID enforcement, `ArrayList` for in-memory transaction history, and `try-with-resources` file I/O (`members.csv` and `transactions.txt`).
- **Transaction Timestamps**: Records exact `LocalDateTime` timestamps for every registration, account opening, deposit, and withdrawal.
- **PIN Authentication**: Requires a 4-digit Member PIN before authorizing any withdrawal.
- **Monthly Mini-Statements**: Parses `transactions.txt` to generate a formatted monthly activity statement for a specific member.
- **Loan Eligibility Validation**: Restricts `LoanAccount` creation to members holding a `SavingsAccount` with at least **KSh 10,000.00**.
