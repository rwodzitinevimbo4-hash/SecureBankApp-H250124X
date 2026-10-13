import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * FileStorage
 * ----------------------------------------------------------------------
 * WHAT IS THIS FILE FOR? 
 * Computer memory is wiped when a program closes. To remember users,
 * accounts and history, we save them into plain text files and read them
 * back the next time the program starts. This class is the ONLY place in
 * the program that touches those files (keeping file code in one place
 * makes it easier to check and secure).
 *
 * The three files live in the "data" folder:
 *   users.txt         username|salt|passwordHash|failedAttempts|lockedUntil
 *   accounts.txt      accountNumber|owner|type|balanceCents
 *   transactions.txt  id|accountNumber|type|amountCents|balanceAfterCents|timestamp
 
 */
public class FileStorage {

    private static final String SEPARATOR = "|";

    private final Path usersFile;
    private final Path accountsFile;
    private final Path transactionsFile;

    /** @param dataDirectory folder that holds the three data files (e.g. "data") */
    public FileStorage(String dataDirectory) throws IOException {
        Path dir = Paths.get(dataDirectory);
        Files.createDirectories(dir); // make the folder if it does not exist yet
        this.usersFile = dir.resolve("users.txt");
        this.accountsFile = dir.resolve("accounts.txt");
        this.transactionsFile = dir.resolve("transactions.txt");
    }

    // =====================================================================
    //  LOADING  (file -> memory)
    // =====================================================================

    /** Reads users.txt and returns a map of username -> User. */
    public Map<String, User> loadUsers() throws IOException {
        Map<String, User> users = new LinkedHashMap<>();
        int lineNo = 0;
        for (String line : readLines(usersFile)) {
            lineNo++;
            if (isIgnorable(line)) continue;
            String[] f = line.split("\\|", -1); // -1 keeps empty trailing pieces
            if (f.length != 5) throw corrupt(usersFile, lineNo);
            try {
                User u = new User(f[0], f[1], f[2], Integer.parseInt(f[3]), Long.parseLong(f[4]));
                users.put(u.getUsername().toLowerCase(), u);
            } catch (NumberFormatException e) {
                throw corrupt(usersFile, lineNo);
            }
        }
        return users;
    }

    /** Reads accounts.txt and returns a map of accountNumber -> Account. */
    public Map<String, Account> loadAccounts() throws IOException {
        Map<String, Account> accounts = new LinkedHashMap<>();
        int lineNo = 0;
        for (String line : readLines(accountsFile)) {
            lineNo++;
            if (isIgnorable(line)) continue;
            String[] f = line.split("\\|", -1);
            if (f.length != 4) throw corrupt(accountsFile, lineNo);
            try {
                long balance = Long.parseLong(f[3]);
                if (balance < 0) throw corrupt(accountsFile, lineNo); // a negative balance is never valid
                Account a;
                if (f[2].equals("SAVINGS")) {
                    a = new SavingsAccount(f[0], f[1], balance);
                } else if (f[2].equals("CURRENT")) {
                    a = new CurrentAccount(f[0], f[1], balance);
                } else {
                    throw corrupt(accountsFile, lineNo); // unknown account type
                }
                accounts.put(a.getAccountNumber(), a);
            } catch (NumberFormatException e) {
                throw corrupt(accountsFile, lineNo);
            }
        }
        return accounts;
    }

    /** Reads transactions.txt and returns the full history, oldest first. */
    public List<Transaction> loadTransactions() throws IOException {
        List<Transaction> list = new ArrayList<>();
        int lineNo = 0;
        for (String line : readLines(transactionsFile)) {
            lineNo++;
            if (isIgnorable(line)) continue;
            String[] f = line.split("\\|", -1);
            if (f.length != 6) throw corrupt(transactionsFile, lineNo);
            try {
                list.add(new Transaction(Long.parseLong(f[0]), f[1], Transaction.Type.valueOf(f[2]),
                        Long.parseLong(f[3]), Long.parseLong(f[4]), f[5]));
            } catch (IllegalArgumentException e) { // also covers NumberFormatException
                throw corrupt(transactionsFile, lineNo);
            }
        }
        return list;
    }

    // =====================================================================
    //  SAVING  (memory -> file)
    // =====================================================================

    public void saveUsers(Map<String, User> users) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("# username|salt|passwordHash|failedAttempts|lockedUntilMillis");
        for (User u : users.values()) {
            lines.add(String.join(SEPARATOR, u.getUsername(), u.getSalt(), u.getPasswordHash(),
                    String.valueOf(u.getFailedAttempts()), String.valueOf(u.getLockedUntilMillis())));
        }
        writeAtomically(usersFile, lines);
    }

    public void saveAccounts(Map<String, Account> accounts) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("# accountNumber|owner|type|balanceCents");
        for (Account a : accounts.values()) {
            lines.add(String.join(SEPARATOR, a.getAccountNumber(), a.getOwnerUsername(),
                    a.getTypeName(), String.valueOf(a.getBalanceCents())));
        }
        writeAtomically(accountsFile, lines);
    }

    public void saveTransactions(List<Transaction> transactions) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("# id|accountNumber|type|amountCents|balanceAfterCents|timestamp");
        for (Transaction t : transactions) {
            lines.add(String.join(SEPARATOR, String.valueOf(t.getId()), t.getAccountNumber(),
                    t.getType().name(), String.valueOf(t.getAmountCents()),
                    String.valueOf(t.getBalanceAfterCents()), t.getTimestamp()));
        }
        writeAtomically(transactionsFile, lines);
    }

    // =====================================================================
    //  Private helper methods
    // =====================================================================

    /** Reads every line of a file. If the file does not exist yet, returns an empty list. */
    private List<String> readLines(Path file) throws IOException {
        if (!Files.exists(file)) return new ArrayList<>();
        return Files.readAllLines(file, StandardCharsets.UTF_8);
    }

    /** Blank lines and lines starting with '#' are comments. */
    private boolean isIgnorable(String line) {
        String t = line.trim();
        return t.isEmpty() || t.startsWith("#");
    }

    /** Builds the error we throw when a data file line is damaged. */
    private IOException corrupt(Path file, int lineNo) {
        return new IOException("Data file " + file.getFileName() + " is damaged at line " + lineNo
                + ". Please restore it from a backup instead of continuing.");
    }

    /**
     * Writes all lines to a temporary file, then swaps it over the real
     * file in a single step (the "atomic" save described at the top).
     */
    private void writeAtomically(Path target, List<String> lines) throws IOException {
        Path temp = target.resolveSibling(target.getFileName() + ".tmp");
        Files.write(temp, lines, StandardCharsets.UTF_8);
        try {
            Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (java.nio.file.AtomicMoveNotSupportedException e) {
            // Some systems cannot do an atomic move; fall back to a normal replace.
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
