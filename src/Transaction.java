/**
 * Transaction
 * ----------------------------------------------------------------------
 * WHAT IS THIS FILE FOR? (plain English)
 * One line in the account's "history book". Every time money is put in or
 * taken out we create a Transaction that records what happened, how much,
 * when, and what the balance was afterwards.
 
 */
public final class Transaction {

    /** The kinds of transaction we support. An 'enum' is a fixed list of allowed choices. */
    public enum Type { OPENING, DEPOSIT, WITHDRAWAL }

    private final long id;                 // unique running number
    private final String accountNumber;    // which account this belongs to
    private final Type type;               // what kind of transaction
    private final long amountCents;        // amount, in cents
    private final long balanceAfterCents;  // the balance after this transaction
    private final String timestamp;        // when it happened (text, e.g. 2026-10-04T19:40:00)

    public Transaction(long id, String accountNumber, Type type,
                       long amountCents, long balanceAfterCents, String timestamp) {
        this.id = id;
        this.accountNumber = accountNumber;
        this.type = type;
        this.amountCents = amountCents;
        this.balanceAfterCents = balanceAfterCents;
        this.timestamp = timestamp;
    }

    public long getId() { return id; }
    public String getAccountNumber() { return accountNumber; }
    public Type getType() { return type; }
    public long getAmountCents() { return amountCents; }
    public long getBalanceAfterCents() { return balanceAfterCents; }
    public String getTimestamp() { return timestamp; }
}
