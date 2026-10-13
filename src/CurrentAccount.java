/**
 * CurrentAccount
 * ----------------------------------------------------------------------
 * A kind of Account meant for everyday spending.
 * Like SavingsAccount it INHERITS from Account, but its rules differ
 * (this difference is called POLYMORPHISM: same method names, different
 * behaviour depending on the account type).

 */
public class CurrentAccount extends Account {

    public CurrentAccount(String accountNumber, String ownerUsername, long balanceCents) {
        super(accountNumber, ownerUsername, balanceCents);
    }

    @Override
    public String getTypeName() { return "CURRENT"; }

    @Override
    public long getMinimumBalanceCents() { return 0; }

    @Override
    public long getMaxWithdrawalCents() { return 1_000_000; } // 10,000.00
}
