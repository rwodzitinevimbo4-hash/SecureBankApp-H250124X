/**
 * User
 * ----------------------------------------------------------------------
 * WHAT IS THIS FILE FOR? (plain English)
 * A "class" is a blueprint. This blueprint describes ONE bank customer:
 * their username, the scrambled password (never the real one), and how
 * many wrong login attempts they have made recently.

 */
public class User {

    /** After this many wrong passwords in a row, the account locks. */
    public static final int MAX_FAILED_ATTEMPTS = 3;

    /** How long a locked user must wait: 5 minutes, in milliseconds. */
    public static final long LOCK_DURATION_MS = 5L * 60 * 1000;

    private final String username;      // 'final' = can never change after creation
    private final String salt;          // random value used when scrambling the password
    private final String passwordHash;  // the scrambled password fingerprint
    private int failedAttempts;         // wrong passwords entered in a row
    private long lockedUntilMillis;     // time (ms since 1970) until which login is blocked; 0 = not locked

    /** Constructor: runs when we write "new User(...)" and fills in the fields. */
    public User(String username, String salt, String passwordHash,
                int failedAttempts, long lockedUntilMillis) {
        this.username = username;
        this.salt = salt;
        this.passwordHash = passwordHash;
        this.failedAttempts = failedAttempts;
        this.lockedUntilMillis = lockedUntilMillis;
    }

    // ---- Getters: read-only access to the private fields ----
    public String getUsername() { return username; }
    public String getSalt() { return salt; }
    public String getPasswordHash() { return passwordHash; }
    public int getFailedAttempts() { return failedAttempts; }
    public long getLockedUntilMillis() { return lockedUntilMillis; }

    /** Is this user currently locked out? Compares the lock time with "now". */
    public boolean isLocked() {
        return System.currentTimeMillis() < lockedUntilMillis;
    }

    /** How many whole seconds remain until the lock ends (0 if not locked). */
    public long secondsUntilUnlock() {
        long remaining = lockedUntilMillis - System.currentTimeMillis();
        return remaining > 0 ? (remaining + 999) / 1000 : 0;
    }

    /** Called after a wrong password. Locks the user once the limit is reached. */
    public void recordFailedLogin() {
        failedAttempts++;
        if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
            lockedUntilMillis = System.currentTimeMillis() + LOCK_DURATION_MS;
            failedAttempts = 0; // start counting again after the lock expires
        }
    }

    /** Called after a successful login: forget previous mistakes. */
    public void recordSuccessfulLogin() {
        failedAttempts = 0;
        lockedUntilMillis = 0;
    }
}
