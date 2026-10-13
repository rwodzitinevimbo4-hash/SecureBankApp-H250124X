import java.io.IOException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * BankService
 * ----------------------------------------------------------------------
 * WHAT IS THIS FILE FOR?
 * This is the "brain" of the bank. It holds all users, accounts and
 * transactions in memory and contains the actual rules: how to register,
 * how to log in, how to open an account, deposit and withdraw.
 */
public class BankService {

    /** A custom error type, used to carry a safe message back to the screen. */
    public static class BankException extends Exception {
        private static final long serialVersionUID = 1L; // version tag Java wants on error classes

        public BankException(String message) {
            super(message);
        }
    }

    private final FileStorage storage;
    private final Map<String, User> users;          // key = lowercase username
    private final Map<String, Account> accounts;    // key = account number
    private final List<Transaction> transactions;   // full history, oldest first
    private final SecureRandom random = new SecureRandom();
    private long nextTransactionId;

    /** Loads everything from the files as soon as the service is created. */
    public BankService(FileStorage storage) throws IOException {
        this.storage = storage;
        this.users = storage.loadUsers();
        this.accounts = storage.loadAccounts();
        this.transactions = storage.loadTransactions();
        long max = 0;
        for (Transaction t : transactions) {
            max = Math.max(max, t.getId());
        }
        this.nextTransactionId = max + 1;
    }

    // =====================================================================
    //  AUTHENTICATION
    // =====================================================================

    /** Creates a new user. Throws BankException with a helpful message if something is wrong. */
    public void register(String username, char[] password) throws BankException {
        String problem = InputValidator.checkUsername(username);
        if (problem != null) throw new BankException(problem);
        problem = InputValidator.checkPasswordStrength(password);
        if (problem != null) throw new BankException(problem);

        String key = username.toLowerCase();
        if (users.containsKey(key)) {
            throw new BankException("That username is already taken.");
        }

        String salt = PasswordHasher.newSalt();
        String hash = PasswordHasher.hash(password, salt);
        User user = new User(username, salt, hash, 0, 0);
        users.put(key, user);
        try {
            storage.saveUsers(users);
        } catch (IOException e) {
            users.remove(key);
            throw new BankException("Could not save your registration. Please try again.");
        }
    }

    /**
     * Checks username + password. Returns the User on success.
     * Throws BankException on any failure (with a deliberately vague message).
     */
    public User login(String username, char[] password) throws BankException {
        User user = (username == null) ? null : users.get(username.toLowerCase());

        if (user == null) {
            PasswordHasher.hash(password, PasswordHasher.newSalt());
            throw new BankException("Invalid username or password.");
        }
        if (user.isLocked()) {
            throw new BankException("Too many failed attempts. Try again in "
                    + user.secondsUntilUnlock() + " seconds.");
        }

        if (PasswordHasher.matches(password, user.getSalt(), user.getPasswordHash())) {
            user.recordSuccessfulLogin();
            saveUsersQuietly();
            return user;
        }

        user.recordFailedLogin();
        saveUsersQuietly();
        if (user.isLocked()) {
            throw new BankException("Too many failed attempts. This user is locked for 5 minutes.");
        }
        throw new BankException("Invalid username or password.");
    }

    // =====================================================================
    //  ACCOUNT MANAGEMENT
    // =====================================================================

    /** Opens a new account for the user, with an optional opening deposit (0 allowed). */
    public Account createAccount(User owner, String typeName, long openingCents) throws BankException {
        if (openingCents < 0 || openingCents > InputValidator.MAX_TRANSACTION_CENTS) {
            throw new BankException("Invalid opening deposit.");
        }
        String number = generateUniqueAccountNumber();
        Account account;
        if ("SAVINGS".equals(typeName)) {
            account = new SavingsAccount(number, owner.getUsername(), openingCents);
        } else if ("CURRENT".equals(typeName)) {
            account = new CurrentAccount(number, owner.getUsername(), openingCents);
        } else {
            throw new BankException("Unknown account type.");
        }
        if (openingCents < account.getMinimumBalanceCents()) {
            throw new BankException("A " + account.getTypeName().toLowerCase() + " account needs an opening deposit of at least "
                    + InputValidator.formatCents(account.getMinimumBalanceCents()) + ".");
        }

        accounts.put(number, account);
        int txCountBefore = transactions.size();
        if (openingCents > 0) {
            transactions.add(newTransaction(number, Transaction.Type.OPENING, openingCents, openingCents));
        }
        try {
            saveAll();
        } catch (IOException e) {
            accounts.remove(number);
            while (transactions.size() > txCountBefore) transactions.remove(transactions.size() - 1);
            throw new BankException("Could not save the new account. Please try again.");
        }
        return account;
    }

    /** Returns only the accounts that belong to this user. */
    public List<Account> getAccountsOf(User user) {
        List<Account> result = new ArrayList<>();
        for (Account a : accounts.values()) {
            if (a.getOwnerUsername().equalsIgnoreCase(user.getUsername())) {
                result.add(a);
            }
        }
        return result;
    }

    // =====================================================================
    //  TRANSACTIONS
    // =====================================================================

    public void deposit(User user, String accountNumber, long cents) throws BankException {
        Account account = findOwnedAccount(user, accountNumber);
        long before = account.getBalanceCents();
        try {
            account.deposit(cents);
        } catch (IllegalArgumentException e) {
            throw new BankException(e.getMessage());
        }
        recordAndSave(account, before, Transaction.Type.DEPOSIT, cents);
    }

    public void withdraw(User user, String accountNumber, long cents) throws BankException {
        Account account = findOwnedAccount(user, accountNumber);
        long before = account.getBalanceCents();
        try {
            account.withdraw(cents);
        } catch (IllegalArgumentException e) {
            throw new BankException(e.getMessage());
        }
        recordAndSave(account, before, Transaction.Type.WITHDRAWAL, cents);
    }

    /** Returns the history of one account (only if the user owns it). */
    public List<Transaction> getHistory(User user, String accountNumber) throws BankException {
        findOwnedAccount(user, accountNumber);
        List<Transaction> result = new ArrayList<>();
        for (Transaction t : transactions) {
            if (t.getAccountNumber().equals(accountNumber)) result.add(t);
        }
        return result;
    }

    // =====================================================================
    //  Private helpers
    // =====================================================================

    /**
     * AUTHORIZATION CHECK. Finds an account AND confirms it belongs to the
     * user. The error message is the same whether the account is missing or
     * belongs to someone else, so nothing is revealed about other people's accounts.
     */
    private Account findOwnedAccount(User user, String accountNumber) throws BankException {
        Account account = InputValidator.isValidAccountNumber(accountNumber) ? accounts.get(accountNumber) : null;
        if (account == null || !account.getOwnerUsername().equalsIgnoreCase(user.getUsername())) {
            throw new BankException("Account not found.");
        }
        return account;
    }

    /** Writes the transaction to history and saves; undoes everything if saving fails. */
    private void recordAndSave(Account account, long balanceBefore, Transaction.Type type, long cents)
            throws BankException {
        Transaction t = newTransaction(account.getAccountNumber(), type, cents, account.getBalanceCents());
        transactions.add(t);
        try {
            saveAll();
        } catch (IOException e) {
            transactions.remove(t);
            account.restoreBalance(balanceBefore);
            throw new BankException("Could not save the transaction, so it was cancelled.");
        }
    }

    private Transaction newTransaction(String accountNumber, Transaction.Type type, long amount, long balanceAfter) {
        String now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).toString();
        return new Transaction(nextTransactionId++, accountNumber, type, amount, balanceAfter, now);
    }

    private String generateUniqueAccountNumber() {
        String number;
        do {
            long n = 1_000_000_000L + (long) (random.nextDouble() * 9_000_000_000L);
            number = String.valueOf(n);
        } while (accounts.containsKey(number));
        return number;
    }

    private void saveAll() throws IOException {
        storage.saveAccounts(accounts);
        storage.saveTransactions(transactions);
    }

    /** Saves users; a failure here is not fatal for the login itself. */
    private void saveUsersQuietly() {
        try {
            storage.saveUsers(users);
        } catch (IOException e) {
            System.err.println("Warning: could not update login records.");
        }
    }
}
