/**
 * SavingsAccount
 * ----------------------------------------------------------------------
 * A kind of Account meant for keeping money safe.
 * "extends Account" = INHERITANCE: it gets all the deposit/withdraw code
 * from Account for free, and only adds what is different.
 *
 * Rules for savings:
 *  - At least 5.00 must always stay in the account.
 *  - You can withdraw at most 1,000.00 at a time.
 */
public class SavingsAccount extends Account {

    public SavingsAccount(String accountNumber, String ownerUsername, long balanceCents) {
        super(accountNumber, ownerUsername, balanceCents); // pass details up to Account
    }

    @Override // "@Override" = we are replacing the general version from Account
    public String getTypeName() { return "SAVINGS"; }

    @Override
    public long getMinimumBalanceCents() { return 500; } // 5.00

    @Override
    public long getMaxWithdrawalCents() { return 100_000; } // 1,000.00
}
