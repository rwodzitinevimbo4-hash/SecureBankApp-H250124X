import java.util.regex.Pattern;

/**
 * InputValidator
 * ----------------------------------------------------------------------
 * WHAT IS THIS FILE FOR? 
 * Anything a person types into the program could be wrong, or even
 * deliberately harmful (for example, trying to sneak in special
 * characters that would corrupt our data files). This class is the
 * "security guard at the door": every piece of typed text must pass
 * through one of these checks BEFORE the rest of the program uses it.
 
 */
public final class InputValidator {

    /** Largest amount allowed in ONE transaction: 1,000,000.00 (stored as cents). */
    public static final long MAX_TRANSACTION_CENTS = 100_000_000L;

    /** Largest balance an account may hold, so numbers can never overflow. */
    public static final long MAX_BALANCE_CENTS = 100_000_000_000L;

    // A "Pattern" is a rule describing what text looks like (a "regular expression").
    // USERNAME: 3 to 20 characters, only letters, digits and underscore.
    private static final Pattern USERNAME_RULE = Pattern.compile("^[A-Za-z0-9_]{3,20}$");

    // MONEY: up to 9 digits, optionally followed by a dot and 1 or 2 digits.
    // Examples that pass: 50   50.5   50.25     Examples that fail: -5  5.123  abc  1e5
    private static final Pattern MONEY_RULE = Pattern.compile("^\\d{1,9}(\\.\\d{1,2})?$");

    // ACCOUNT NUMBER: exactly 10 digits.
    private static final Pattern ACCOUNT_NUMBER_RULE = Pattern.compile("^\\d{10}$");

    /** Private constructor: nobody should create an InputValidator object. */
    private InputValidator() {
    }

    /**
     * Checks a username. Returns null if it is fine, otherwise returns a
     * human-friendly message explaining the problem.
     */
    public static String checkUsername(String username) {
        if (username == null || !USERNAME_RULE.matcher(username).matches()) {
            return "Username must be 3-20 characters: letters, digits or underscore only.";
        }
        return null; // null means "no problem found"
    }

    /**
     * Checks that a password is strong enough.
     * Rules: at least 8 characters, with an uppercase letter, a lowercase
     * letter and a digit. Longer + mixed passwords are much harder to guess.
     */
    public static String checkPasswordStrength(char[] password) {
        if (password == null || password.length < 8) {
            return "Password must be at least 8 characters long.";
        }
        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        for (char c : password) { // look at each character one by one
            if (Character.isUpperCase(c)) hasUpper = true;
            if (Character.isLowerCase(c)) hasLower = true;
            if (Character.isDigit(c)) hasDigit = true;
        }
        if (!(hasUpper && hasLower && hasDigit)) {
            return "Password needs an uppercase letter, a lowercase letter and a digit.";
        }
        return null;
    }

    /** Returns true if the text looks like a valid 10-digit account number. */
    public static boolean isValidAccountNumber(String accountNumber) {
        return accountNumber != null && ACCOUNT_NUMBER_RULE.matcher(accountNumber).matches();
    }

    /**
     * Turns text such as "25.50" into a whole number of cents (2550).
     *
     * WHY CENTS? Computers store decimals like 0.1 slightly inaccurately
     * (0.1 + 0.2 = 0.30000000000000004). That is unacceptable for money, so
     * we store money as whole cents (a "long" integer) and only add the
     * decimal point when displaying.
     *
     * @return the amount in cents, or -1 if the text is not a valid positive amount.
     */
    public static long parseMoneyToCents(String text) {
        if (text == null) return -1;
        String trimmed = text.trim();
        if (!MONEY_RULE.matcher(trimmed).matches()) return -1;

        String[] parts = trimmed.split("\\."); // split "25.5" into "25" and "5"
        long dollars = Long.parseLong(parts[0]);
        long cents = 0;
        if (parts.length == 2) {
            // "5" means 50 cents, "05" means 5 cents - so pad a single digit.
            String c = parts[1].length() == 1 ? parts[1] + "0" : parts[1];
            cents = Long.parseLong(c);
        }
        long total = dollars * 100 + cents;
        if (total <= 0 || total > MAX_TRANSACTION_CENTS) return -1; // must be positive and not huge
        return total;
    }

    /** Turns cents (2550) back into display text ("25.50"). */
    public static String formatCents(long cents) {
        return String.format("%d.%02d", cents / 100, cents % 100);
    }
}
