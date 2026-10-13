# Secure Banking Application

## Description
A console-based banking application written in Java that lets users register, log in, open accounts, deposit and withdraw money.
It demonstrates Object-Oriented Programming (OOP) and secure coding practices, with all data saved in plain text files (no database).

## Student Details
- **Name:** [Your Full Name]
- **Registration Number:** [Your Reg Number]

## Features
- **User authentication:** register and log in; passwords are hashed with PBKDF2 (HMAC-SHA256, 600,000 iterations as recommended by OWASP) and a unique random salt, never stored in plain text.
- **Account lockout:** 3 wrong passwords in a row locks the user for 5 minutes (persisted in the file).
- **Account management:** create Savings or Current accounts (random unique 10-digit numbers) and view balances.
- **Transactions:** deposit and withdraw, with per-account-type rules (minimum balance, withdrawal limit) and a full transaction history per account.
- **Data persistence:** users, accounts and transactions are saved to `data/users.txt`, `data/accounts.txt`, `data/transactions.txt` and loaded on start-up.
- **Secure coding:** input whitelisting, money stored as whole cents (no floating-point errors), authorization checks (users only access their own accounts), generic login errors, atomic file writes, rollback if saving fails, refusal to start on a corrupted data file, passwords wiped from memory.

## OOP Concepts Used
| Concept | Where |
|---|---|
| Abstraction | `Account` is abstract (a template for specific account types) |
| Inheritance | `SavingsAccount` and `CurrentAccount` extend `Account` |
| Polymorphism | Each account type overrides minimum balance and withdrawal limit |
| Encapsulation | Private fields; balance changes only through `deposit()` / `withdraw()` |
| Immutability | `Transaction` fields are all `final` |

## Project Structure
```
SecureBankApp/
├── src/
│   ├── Main.java            # menus and keyboard input
│   ├── BankService.java     # banking rules (register, login, deposit...)
│   ├── Account.java         # abstract account
│   ├── SavingsAccount.java
│   ├── CurrentAccount.java
│   ├── User.java
│   ├── Transaction.java
│   ├── FileStorage.java     # reads/writes the text files
│   ├── PasswordHasher.java  # salted PBKDF2 hashing
│   └── InputValidator.java  # input checks, money <-> cents
└── data/
    ├── users.txt
    ├── accounts.txt
    └── transactions.txt
```

## How to Run
Requires Java 11 or newer. From the project folder:
```bash
javac -d out src/*.java
java -cp out Main
```
Run it from the project root so the program finds the `data/` folder.

## Data File Formats
Fields are separated by `|`; lines starting with `#` are comments.
- `users.txt`: `username|salt|passwordHash|failedAttempts|lockedUntilMillis`
- `accounts.txt`: `accountNumber|owner|type|balanceCents`
- `transactions.txt`: `id|accountNumber|type|amountCents|balanceAfterCents|timestamp`

## Account Rules
| | Savings | Current |
|---|---|---|
| Minimum balance | 5.00 | 0.00 |
| Max per withdrawal | 1,000.00 | 10,000.00 |

Maximum single transaction: 1,000,000.00.
