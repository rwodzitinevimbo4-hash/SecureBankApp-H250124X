import java.io.Console;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;

/**
 * Main  -  the program starts here
 * ----------------------------------------------------------------------
 * WHAT IS THIS FILE FOR? 
 * This file draws the menus on the screen, reads what the person types,
 * and passes the requests to BankService (the "brain"). It contains no
 * banking rules itself - only the conversation with the user.

 */
public class Main {

    // Scanner reads typed text from the keyboard.
    private static final Scanner INPUT = new Scanner(System.in);

    // The user who is logged in right now (null = nobody).
    private static User currentUser = null;

    public static void main(String[] args) {
        BankService bank;
        try {
            // All data files live in the folder called "data".
            bank = new BankService(new FileStorage("data"));
        } catch (IOException e) {
            // Stop safely if the files are unreadable or damaged.
            System.err.println("Cannot start: " + e.getMessage());
            return;
        }

        System.out.println("=====================================");
        System.out.println("      SECURE BANKING APPLICATION");
        System.out.println("=====================================");

        boolean running = true;
        while (running) {                       // keep showing menus until the user exits
            if (currentUser == null) {
                running = showLoggedOutMenu(bank);
            } else {
                showLoggedInMenu(bank);
            }
        }
        System.out.println("Goodbye!");
    }

    // =====================================================================
    //  MENUS
    // =====================================================================

    /** Menu shown before login. Returns false when the user chooses to exit. */
    private static boolean showLoggedOutMenu(BankService bank) {
        System.out.println("\n--- MAIN MENU ---");
        System.out.println("1. Register");
        System.out.println("2. Login");
        System.out.println("3. Exit");
        String choice = ask("Choose an option: ");
        if (choice == null) return false; // keyboard input ended (e.g. Ctrl+D)

        switch (choice) {
            case "1": doRegister(bank); break;
            case "2": doLogin(bank); break;
            case "3": return false;
            default: System.out.println("Please enter 1, 2 or 3.");
        }
        return true;
    }

    /** Menu shown after a successful login. */
    private static void showLoggedInMenu(BankService bank) {
        System.out.println("\n--- ACCOUNT MENU (user: " + currentUser.getUsername() + ") ---");
        System.out.println("1. Create new account");
        System.out.println("2. View my accounts and balances");
        System.out.println("3. Deposit");
        System.out.println("4. Withdraw");
        System.out.println("5. View transaction history");
        System.out.println("6. Logout");
        String choice = ask("Choose an option: ");
        if (choice == null) { currentUser = null; return; }

        switch (choice) {
            case "1": doCreateAccount(bank); break;
            case "2": doViewAccounts(bank); break;
            case "3": doMoneyMove(bank, true); break;
            case "4": doMoneyMove(bank, false); break;
            case "5": doHistory(bank); break;
            case "6":
                currentUser = null;
                System.out.println("You have been logged out.");
                break;
            default: System.out.println("Please enter a number from 1 to 6.");
        }
    }

    // =====================================================================
    //  ACTIONS (one method per menu option)
    // =====================================================================

    private static void doRegister(BankService bank) {
        String username = ask("Choose a username (3-20 letters/digits/_): ");
        if (username == null) return;
        char[] password = askPassword("Choose a password (8+ chars, upper, lower, digit): ");
        char[] confirm = askPassword("Repeat the password: ");
        try {
            if (password == null || confirm == null || !java.util.Arrays.equals(password, confirm)) {
                System.out.println("Passwords did not match.");
                return;
            }
            bank.register(username.trim(), password);
            System.out.println("Registration successful. You can now log in.");
        } catch (BankService.BankException e) {
            System.out.println("Error: " + e.getMessage());
        } finally {
            // 'finally' ALWAYS runs - we use it to wipe passwords from memory.
            PasswordHasher.wipe(password);
            PasswordHasher.wipe(confirm);
        }
    }

    private static void doLogin(BankService bank) {
        String username = ask("Username: ");
        if (username == null) return;
        char[] password = askPassword("Password: ");
        if (password == null) return;
        try {
            currentUser = bank.login(username.trim(), password);
            System.out.println("Welcome, " + currentUser.getUsername() + "!");
        } catch (BankService.BankException e) {
            System.out.println("Error: " + e.getMessage());
        } finally {
            PasswordHasher.wipe(password);
        }
    }

    private static void doCreateAccount(BankService bank) {
        System.out.println("Account types:  1) Savings (min balance 5.00)   2) Current");
        String type = ask("Choose type (1 or 2): ");
        if (type == null) return;
        String typeName;
        if (type.equals("1")) typeName = "SAVINGS";
        else if (type.equals("2")) typeName = "CURRENT";
        else { System.out.println("Invalid type."); return; }

        String amountText = ask("Opening deposit (e.g. 50.00, or 0 for none): ");
        if (amountText == null) return;
        long cents;
        if (amountText.trim().equals("0")) {
            cents = 0;
        } else {
            cents = InputValidator.parseMoneyToCents(amountText);
            if (cents < 0) { System.out.println("Invalid amount."); return; }
        }
        try {
            Account a = bank.createAccount(currentUser, typeName, cents);
            System.out.println("Account created! Your account number is " + a.getAccountNumber());
        } catch (BankService.BankException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void doViewAccounts(BankService bank) {
        List<Account> list = bank.getAccountsOf(currentUser);
        if (list.isEmpty()) {
            System.out.println("You have no accounts yet. Choose option 1 to create one.");
            return;
        }
        System.out.println("\nAccount number | Type    | Balance");
        for (Account a : list) {
            System.out.printf("%-14s | %-7s | %s%n", a.getAccountNumber(), a.getTypeName(),
                    InputValidator.formatCents(a.getBalanceCents()));
        }
    }

    /** Handles both deposit (isDeposit = true) and withdrawal (false), since the steps are almost identical. */
    private static void doMoneyMove(BankService bank, boolean isDeposit) {
        String number = ask("Account number: ");
        if (number == null) return;
        String amountText = ask("Amount (e.g. 25.50): ");
        if (amountText == null) return;
        long cents = InputValidator.parseMoneyToCents(amountText);
        if (cents < 0) {
            System.out.println("Invalid amount. Use a positive number with at most 2 decimals (max 1000000.00).");
            return;
        }
        try {
            if (isDeposit) bank.deposit(currentUser, number.trim(), cents);
            else bank.withdraw(currentUser, number.trim(), cents);
            System.out.println((isDeposit ? "Deposit" : "Withdrawal") + " successful.");
        } catch (BankService.BankException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void doHistory(BankService bank) {
        String number = ask("Account number: ");
        if (number == null) return;
        try {
            List<Transaction> list = bank.getHistory(currentUser, number.trim());
            if (list.isEmpty()) {
                System.out.println("No transactions yet.");
                return;
            }
            System.out.println("\nID | Date/time           | Type       | Amount     | Balance after");
            for (Transaction t : list) {
                System.out.printf("%-2d | %-19s | %-10s | %-10s | %s%n", t.getId(),
                        t.getTimestamp().replace('T', ' '), t.getType(),
                        InputValidator.formatCents(t.getAmountCents()),
                        InputValidator.formatCents(t.getBalanceAfterCents()));
            }
        } catch (BankService.BankException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    // =====================================================================
    //  KEYBOARD HELPERS
    // =====================================================================

    /** Prints a question and returns the typed line, or null if input has ended. */
    private static String ask(String prompt) {
        System.out.print(prompt);
        if (!INPUT.hasNextLine()) return null;
        return INPUT.nextLine();
    }

    /**
     * Reads a password. In a real terminal, System.console() hides the
     * characters as you type. Some environments (like an IDE) have no
     * console, so we fall back to normal (visible) typing there.
     */
    private static char[] askPassword(String prompt) {
        Console console = System.console();
        if (console != null) {
            return console.readPassword(prompt);
        }
        String text = ask(prompt + "(input will be visible) ");
        return text == null ? null : text.toCharArray();
    }
}
