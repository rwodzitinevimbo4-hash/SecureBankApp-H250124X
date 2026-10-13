/**
 * Account  (ABSTRACT class)
 * ----------------------------------------------------------------------
 * WHAT IS THIS FILE FOR? 
 * This describes what EVERY bank account has in common: an account
 * number, an owner, a balance, and the ability to deposit and withdraw.

 */
public abstract class Account {

    private final String accountNumber; // 10-digit number, never changes
    private final String ownerUsername; // which user owns this account
    private long balanceCents;          // money in the account, in cents (private!)

    protected Account(String accountNumber, String ownerUsername, long balanceCents) {
        this.accountNumber = accountNumber;
        this.ownerUsername = ownerUsername;
        this.balanceCents = balanceCents;
    }

    // ---- Methods that each specific account type MUST provide ----

    /** Text name of the account type, e.g. "SAVINGS". Saved in the data file. */
    public abstract String getTypeName();

    /** The least money that must remain in the account after a withdrawal. */
    public abstract long getMinimumBalanceCents();

    /** The most that may be withdrawn in a single transaction. */
    public abstract long getMaxWithdrawalCents();

    // ---- Shared behaviour (same for every account type) ----

    public String getAccountNumber() { return accountNumber; }
    public String getOwnerUsername() { return ownerUsername; }
    public long getBalanceCents() { return balanceCents; }

    /**
     * Adds money. Throws an exception (an "error signal") if the amount is
     * not allowed. The caller must handle or report that error.
     */
    public void deposit(long amountCents) {
        if (amountCents <= 0) {
            throw new IllegalArgumentException("Deposit must be greater than zero.");
        }
        // Make sure the balance can never grow past our safe maximum.
        if (balanceCents + amountCents > InputValidator.MAX_BALANCE_CENTS) {
            throw new IllegalArgumentException("This deposit would exceed the maximum allowed balance.");
        }
        balanceCents += amountCents;
    }

    /** Removes money, but only if every rule is satisfied. */
    public void withdraw(long amountCents) {
        if (amountCents <= 0) {
            throw new IllegalArgumentException("Withdrawal must be greater than zero.");
        }
        if (amountCents > getMaxWithdrawalCents()) {
            throw new IllegalArgumentException("Limit per withdrawal for this account type is "
                    + InputValidator.formatCents(getMaxWithdrawalCents()) + ".");
        }
        if (balanceCents - amountCents < getMinimumBalanceCents()) {
            throw new IllegalArgumentException("Insufficient funds. Minimum balance of "
                    + InputValidator.formatCents(getMinimumBalanceCents()) + " must remain.");
        }
        balanceCents -= amountCents;
    }

    /**
     * Puts the balance back to an earlier value. Used ONLY when saving to
     * disk fails after a change, so memory and files never disagree.
     * "protected/package" level: not meant for general use.
     */
    void restoreBalance(long previousBalanceCents) {
        this.balanceCents = previousBalanceCents;
    }
}
