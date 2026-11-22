import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.InputMismatchException;
import java.util.Map;
import java.util.Scanner;

// ============================================================================
// 1. CUSTOM EXCEPTION
// ============================================================================
class InsufficientFundsException extends Exception {
    public InsufficientFundsException(String message) {
        super(message);
    }
}

// ============================================================================
// 2. INTERFACE
// ============================================================================
interface Auditable {
    void logTransaction(String message);
}

// ============================================================================
// 3. TRANSACTION MODEL (Stretch Goal 1: LocalDateTime Timestamps)
// ============================================================================
class Transaction {
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final LocalDateTime timestamp;
    private final String type;
    private final double amount;
    private final double balanceAfter;

    public Transaction(String type, double amount, double balanceAfter) {
        this.timestamp = LocalDateTime.now();
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getFormattedTimestamp() {
        return timestamp.format(FORMATTER);
    }

    public String getType() {
        return type;
    }

    public double getAmount() {
        return amount;
    }

    public double getBalanceAfter() {
        return balanceAfter;
    }

    @Override
    public String toString() {
        return String.format("[%s] %-10s | Amount: KSh %,.2f | New Balance: KSh %,.2f",
                getFormattedTimestamp(), type, amount, balanceAfter);
    }
}

// ============================================================================
// 4. ABSTRACT BASE CLASS (Encapsulation, Inheritance & Polymorphism)
// ============================================================================
abstract class Account implements Auditable {
    private final String memberId;
    private final String accountNumber;
    private double balance;
    private final ArrayList<Transaction> transactionHistory;

    public Account(String memberId, String accountNumber, double initialBalance) {
        this.memberId = memberId;
        this.accountNumber = accountNumber;
        this.balance = Math.max(0.0, initialBalance);
        this.transactionHistory = new ArrayList<>();
    }

    public String getMemberId() {
        return memberId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public double getBalance() {
        return balance;
    }

    public ArrayList<Transaction> getTransactionHistory() {
        return transactionHistory;
    }

    public void deposit(double amount) {
        if (amount <= 0) {
            System.out.println("Error: Deposit amount must be greater than KSh 0.");
            return;
        }
        balance += amount;
        Transaction tx = new Transaction("DEPOSIT", amount, balance);
        transactionHistory.add(tx);
        logTransaction(formatLogEntry(tx));
    }

    public void withdraw(double amount) throws InsufficientFundsException {
        if (amount <= 0) {
            System.out.println("Error: Withdrawal amount must be greater than KSh 0.");
            return;
        }
        if (amount > balance) {
            throw new InsufficientFundsException(
                    String.format("Insufficient funds. Available balance is KSh %,.2f", balance));
        }
        balance -= amount;
        Transaction tx = new Transaction("WITHDRAWAL", amount, balance);
        transactionHistory.add(tx);
        logTransaction(formatLogEntry(tx));
    }

    private String formatLogEntry(Transaction tx) {
        return String.format("[%s] | MEMBER:%s | ACC:%s | %s | Amount: KSh %.2f | Balance: KSh %.2f",
                tx.getFormattedTimestamp(),
                memberId,
                accountNumber,
                tx.getType(),
                tx.getAmount(),
                tx.getBalanceAfter());
    }

    // Abstract method overridden differently in SavingsAccount and LoanAccount
    public abstract double calculateInterest();

    @Override
    public void logTransaction(String message) {
        try (FileWriter fw = new FileWriter(SaccoManager.TRANSACTIONS_FILE, true);
             PrintWriter pw = new PrintWriter(fw)) {
            pw.println(message);
        } catch (IOException e) {
            System.out.println("Error writing to transaction log: " + e.getMessage());
        }
    }
}

// ============================================================================
// 5. CONCRETE SUBCLASSES
// ============================================================================
class SavingsAccount extends Account {
    public SavingsAccount(String memberId, String accountNumber, double initialBalance) {
        super(memberId, accountNumber, initialBalance);
    }

    @Override
    public double calculateInterest() {
        return getBalance() * 0.05; // 5% annual interest
    }
}

class LoanAccount extends Account {
    public LoanAccount(String memberId, String accountNumber, double initialBalance) {
        super(memberId, accountNumber, initialBalance);
    }

    @Override
    public double calculateInterest() {
        return getBalance() * 0.12; // 12% loan interest
    }
}

// ============================================================================
// 6. DOMAIN CLASS: MEMBER (Includes PIN for Stretch Goal 2)
// ============================================================================
class Member {
    private final String memberId;
    private String name;
    private final String nationalId;
    private String pin;
    private final Map<String, Account> accounts;

    public Member(String memberId, String name, String nationalId, String pin) {
        this.memberId = memberId;
        this.name = name;
        this.nationalId = nationalId;
        this.pin = pin;
        this.accounts = new HashMap<>();
    }

    public String getMemberId() {
        return memberId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name != null && !name.trim().isEmpty()) {
            this.name = name.trim();
        }
    }

    public String getNationalId() {
        return nationalId;
    }

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        if (pin != null && pin.matches("\\d{4}")) {
            this.pin = pin;
        } else {
            throw new IllegalArgumentException("PIN must be exactly 4 digits.");
        }
    }

    public boolean verifyPin(String inputPin) {
        return this.pin != null && this.pin.equals(inputPin);
    }

    public Map<String, Account> getAccounts() {
        return accounts;
    }

    public void addAccount(String type, Account account) {
        accounts.put(type, account);
    }
}

// ============================================================================
// 7. MAIN APPLICATION CLASS
// ============================================================================
public class SaccoManager {
    private static final HashMap<String, Member> membersDb = new HashMap<>();
    private static final HashSet<String> registeredNationalIds = new HashSet<>();

    static final String MEMBERS_FILE = "members.csv";
    static final String TRANSACTIONS_FILE = "transactions.txt";

    private static final double MIN_SAVINGS_FOR_LOAN = 10000.00;
    private static final DateTimeFormatter LOG_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        loadData();

        int choice;
        do {
            System.out.println("\n==========================================");
            System.out.println("     M-KOPO SACCO MANAGEMENT SYSTEM       ");
            System.out.println("==========================================");
            System.out.println("1. Register New Member");
            System.out.println("2. Open an Account");
            System.out.println("3. Deposit Money");
            System.out.println("4. Withdraw Money (PIN Required)");
            System.out.println("5. View Member Details");
            System.out.println("6. Generate Member Mini-Statement");
            System.out.println("7. Exit & Save");

            choice = readInt(scanner, "Enter choice (1-7): ");

            switch (choice) {
                case 1:
                    registerMember(scanner);
                    break;
                case 2:
                    openAccount(scanner);
                    break;
                case 3:
                    depositMoney(scanner);
                    break;
                case 4:
                    withdrawMoney(scanner);
                    break;
                case 5:
                    viewMemberDetails(scanner);
                    break;
                case 6:
                    generateMiniStatement(scanner);
                    break;
                case 7:
                    saveData();
                    System.out.println("Data saved successfully. Exiting M-Kopo SACCO System. Goodbye!");
                    break;
                default:
                    System.out.println("Invalid choice. Please enter a number between 1 and 7.");
            }
        } while (choice != 7);

        scanner.close();
    }

    // ------------------------------------------------------------------------
    // MENU OPTION 1: REGISTER MEMBER (With PIN & Audit Logging)
    // ------------------------------------------------------------------------
    private static void registerMember(Scanner scanner) {
        System.out.print("Enter Member ID (e.g., M-001): ");
        String id = scanner.nextLine().trim().toUpperCase();

        if (id.isEmpty()) {
            System.out.println("Error: Member ID cannot be empty.");
            return;
        }
        if (membersDb.containsKey(id)) {
            System.out.println("Error: Member ID '" + id + "' already exists.");
            return;
        }

        System.out.print("Enter Full Name: ");
        String name = scanner.nextLine().trim().replace(",", " ");
        if (name.isEmpty()) {
            System.out.println("Error: Name cannot be empty.");
            return;
        }

        System.out.print("Enter National ID: ");
        String nationalId = scanner.nextLine().trim();
        if (nationalId.isEmpty()) {
            System.out.println("Error: National ID cannot be empty.");
            return;
        }
        if (registeredNationalIds.contains(nationalId)) {
            System.out.println("Error: A member with National ID '" + nationalId + "' is already registered.");
            return;
        }

        // Stretch Goal 2: Set a 4-digit security PIN
        String pin;
        while (true) {
            System.out.print("Create a 4-digit Security PIN: ");
            pin = scanner.nextLine().trim();
            if (pin.matches("\\d{4}")) {
                break;
            }
            System.out.println("Invalid PIN. Please enter exactly 4 numeric digits (e.g., 1234).");
        }

        Member newMember = new Member(id, name, nationalId, pin);
        membersDb.put(id, newMember);
        registeredNationalIds.add(nationalId);

        // Log member registration to transactions.txt with timestamp
        logSystemEvent(String.format("[%s] | MEMBER:%s | ACC:N/A | REGISTRATION | Registered %s (National ID: %s)",
                LocalDateTime.now().format(LOG_FORMATTER), id, name, nationalId));

        System.out.println("Success: Member " + name + " (" + id + ") registered successfully!");
    }

    // ------------------------------------------------------------------------
    // MENU OPTION 2: OPEN ACCOUNT (Stretch Goal 4: Loan Eligibility Check)
    // ------------------------------------------------------------------------
    private static void openAccount(Scanner scanner) {
        System.out.print("Enter Member ID: ");
        String id = scanner.nextLine().trim().toUpperCase();

        Member member = membersDb.get(id);
        if (member == null) {
            System.out.println("Error: Member not found.");
            return;
        }

        System.out.println("1. Savings Account (5% interest)");
        System.out.println("2. Loan Account (12% interest - Requires Savings >= KSh 10,000)");
        int type = readInt(scanner, "Choose account type (1 or 2): ");

        String accountNumber = id + "-" + (1000 + (int) (System.currentTimeMillis() % 9000));

        if (type == 1) {
            if (member.getAccounts().containsKey("Savings")) {
                System.out.println("Error: Member already holds an active Savings Account.");
                return;
            }
            member.addAccount("Savings", new SavingsAccount(id, accountNumber, 0.0));
            logSystemEvent(String.format("[%s] | MEMBER:%s | ACC:%s | ACCOUNT_OPENED | Opened Savings Account",
                    LocalDateTime.now().format(LOG_FORMATTER), id, accountNumber));
            System.out.println("Success: Savings Account (" + accountNumber + ") opened!");

        } else if (type == 2) {
            if (member.getAccounts().containsKey("Loan")) {
                System.out.println("Error: Member already holds an active Loan Account.");
                return;
            }

            // Stretch Goal 4: Enforce KSh 10,000 minimum Savings balance
            Account savings = member.getAccounts().get("Savings");
            if (savings == null) {
                System.out.println("ELIGIBILITY DENIED: Member must first open a Savings Account and maintain at least KSh 10,000.00.");
                return;
            }
            if (savings.getBalance() < MIN_SAVINGS_FOR_LOAN) {
                System.out.printf("ELIGIBILITY DENIED: Current Savings balance is KSh %,.2f. Minimum required is KSh %,.2f.%n",
                        savings.getBalance(), MIN_SAVINGS_FOR_LOAN);
                return;
            }

            member.addAccount("Loan", new LoanAccount(id, accountNumber, 0.0));
            logSystemEvent(String.format("[%s] | MEMBER:%s | ACC:%s | ACCOUNT_OPENED | Opened Loan Account",
                    LocalDateTime.now().format(LOG_FORMATTER), id, accountNumber));
            System.out.println("Success: Loan Account (" + accountNumber + ") opened!");

        } else {
            System.out.println("Invalid account type selected.");
        }
    }

    // ------------------------------------------------------------------------
    // MENU OPTION 3: DEPOSIT MONEY
    // ------------------------------------------------------------------------
    private static void depositMoney(Scanner scanner) {
        System.out.print("Enter Member ID: ");
        String id = scanner.nextLine().trim().toUpperCase();
        Member member = membersDb.get(id);

        if (member == null) {
            System.out.println("Error: Member not found.");
            return;
        }

        Account account = selectMemberAccount(scanner, member);
        if (account == null) {
            return;
        }

        double amount = readDouble(scanner, "Enter amount to deposit (KSh): ");
        double previousBalance = account.getBalance();
        account.deposit(amount);

        if (account.getBalance() > previousBalance) {
            System.out.printf("Success! New Balance: KSh %,.2f%n", account.getBalance());
        }
    }

    // ------------------------------------------------------------------------
    // MENU OPTION 4: WITHDRAW MONEY (Stretch Goal 2: PIN Authentication)
    // ------------------------------------------------------------------------
    private static void withdrawMoney(Scanner scanner) {
        System.out.print("Enter Member ID: ");
        String id = scanner.nextLine().trim().toUpperCase();
        Member member = membersDb.get(id);

        if (member == null) {
            System.out.println("Error: Member not found.");
            return;
        }

        // Authenticate Member PIN before permitting withdrawal
        System.out.print("Enter 4-digit Member PIN to authorize withdrawal: ");
        String enteredPin = scanner.nextLine().trim();
        if (!member.verifyPin(enteredPin)) {
            System.out.println("AUTHENTICATION FAILED: Incorrect PIN. Withdrawal cancelled.");
            return;
        }

        Account account = selectMemberAccount(scanner, member);
        if (account == null) {
            return;
        }

        double amount = readDouble(scanner, "Enter amount to withdraw (KSh): ");
        try {
            account.withdraw(amount);
            System.out.printf("Success! New Balance: KSh %,.2f%n", account.getBalance());
        } catch (InsufficientFundsException e) {
            System.out.println("TRANSACTION FAILED: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------------
    // MENU OPTION 5: VIEW MEMBER DETAILS
    // ------------------------------------------------------------------------
    private static void viewMemberDetails(Scanner scanner) {
        System.out.print("Enter Member ID: ");
        String id = scanner.nextLine().trim().toUpperCase();
        Member member = membersDb.get(id);

        if (member == null) {
            System.out.println("Error: Member not found.");
            return;
        }

        System.out.println("\n--- MEMBER DETAILS ---");
        System.out.println("Name:        " + member.getName());
        System.out.println("Member ID:   " + member.getMemberId());
        System.out.println("National ID: " + member.getNationalId());

        Map<String, Account> accounts = member.getAccounts();
        if (accounts.isEmpty()) {
            System.out.println("Accounts:    No accounts opened yet.");
        } else {
            for (Map.Entry<String, Account> entry : accounts.entrySet()) {
                Account acc = entry.getValue();
                System.out.printf("Account: %-7s | No: %s | Balance: KSh %,.2f | Est. Interest: KSh %,.2f%n",
                        entry.getKey(),
                        acc.getAccountNumber(),
                        acc.getBalance(),
                        acc.calculateInterest());
            }
        }
    }

    // ------------------------------------------------------------------------
    // MENU OPTION 6: MINI-STATEMENT (Stretch Goal 3: Reads transactions.txt)
    // ------------------------------------------------------------------------
    private static void generateMiniStatement(Scanner scanner) {
        System.out.print("Enter Member ID: ");
        String id = scanner.nextLine().trim().toUpperCase();
        Member member = membersDb.get(id);

        if (member == null) {
            System.out.println("Error: Member not found.");
            return;
        }

        File logFile = new File(TRANSACTIONS_FILE);
        if (!logFile.exists()) {
            System.out.println("No transaction history file found yet.");
            return;
        }

        String currentMonthPrefix = "[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        ArrayList<String> monthlyRecords = new ArrayList<>();
        ArrayList<String> allMemberRecords = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("| MEMBER:" + id + " |")) {
                    allMemberRecords.add(line);
                    if (line.startsWith(currentMonthPrefix)) {
                        monthlyRecords.add(line);
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Error reading transaction logs: " + e.getMessage());
            return;
        }

        System.out.println("\n==========================================================================");
        System.out.println("                     M-KOPO SACCO MINI-STATEMENT                          ");
        System.out.println("==========================================================================");
        System.out.println("Member Name:   " + member.getName() + " (" + member.getMemberId() + ")");
        System.out.println("Statement Period: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM yyyy")));
        System.out.println("Generated On:  " + LocalDateTime.now().format(LOG_FORMATTER));
        System.out.println("--------------------------------------------------------------------------");

        if (member.getAccounts().isEmpty()) {
            System.out.println("Active Accounts: None");
        } else {
            for (Map.Entry<String, Account> entry : member.getAccounts().entrySet()) {
                Account acc = entry.getValue();
                System.out.printf("%-8s Account (%s) -> Current Balance: KSh %,.2f%n",
                        entry.getKey(), acc.getAccountNumber(), acc.getBalance());
            }
        }

        System.out.println("--------------------------------------------------------------------------");
        System.out.println("MONTHLY ACTIVITY LOG:");

        if (!monthlyRecords.isEmpty()) {
            for (String record : monthlyRecords) {
                System.out.println("  " + record);
            }
        } else if (!allMemberRecords.isEmpty()) {
            System.out.println("  (No entries for current month; showing all historical entries)");
            for (String record : allMemberRecords) {
                System.out.println("  " + record);
            }
        } else {
            System.out.println("  No transactions found for member " + id + ".");
        }
        System.out.println("==========================================================================");
    }

    // ------------------------------------------------------------------------
    // HELPER METHODS (Account Lookup, Input Validation & System Logging)
    // ------------------------------------------------------------------------
    private static Account selectMemberAccount(Scanner scanner, Member member) {
        if (member.getAccounts().isEmpty()) {
            System.out.println("Error: This member has not opened any accounts yet.");
            return null;
        }

        System.out.print("Enter account type (Savings / Loan): ");
        String rawType = scanner.nextLine().trim();

        // Normalize capitalization so "savings" or "SAVINGS" works cleanly
        String normalizedType = rawType.equalsIgnoreCase("Savings") ? "Savings"
                : rawType.equalsIgnoreCase("Loan") ? "Loan" : rawType;

        Account account = member.getAccounts().get(normalizedType);
        if (account == null) {
            System.out.println("Error: '" + rawType + "' account not found for this member.");
            return null;
        }
        return account;
    }

    private static int readInt(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                int value = scanner.nextInt();
                scanner.nextLine(); // Clear newline buffer
                return value;
            } catch (InputMismatchException e) {
                System.out.println("Invalid input. Please enter a whole number.");
                scanner.nextLine(); // Discard invalid token
            }
        }
    }

    private static double readDouble(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                double value = scanner.nextDouble();
                scanner.nextLine(); // Clear newline buffer
                return value;
            } catch (InputMismatchException e) {
                System.out.println("Invalid input. Please enter a valid numeric amount.");
                scanner.nextLine(); // Discard invalid token
            }
        }
    }

    private static void logSystemEvent(String entry) {
        try (FileWriter fw = new FileWriter(TRANSACTIONS_FILE, true);
             PrintWriter pw = new PrintWriter(fw)) {
            pw.println(entry);
        } catch (IOException e) {
            System.out.println("Error writing event to log: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------------
    // FILE I/O PERSISTENCE (members.csv)
    // ------------------------------------------------------------------------
    private static void loadData() {
        File file = new File(MEMBERS_FILE);
        if (!file.exists()) {
            System.out.println("No existing data file found. Starting with an empty database.");
            return;
        }

        try (Scanner fileScanner = new Scanner(file)) {
            while (fileScanner.hasNextLine()) {
                String line = fileScanner.nextLine().trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split(",");
                // Expected CSV format: MemberId,Name,NationalId,PIN,AccountType,AccountNumber,Balance
                if (parts.length == 7) {
                    String memberId = parts[0];
                    String name = parts[1];
                    String nationalId = parts[2];
                    String pin = parts[3];
                    String accType = parts[4];
                    String accNumber = parts[5];
                    double balance = Double.parseDouble(parts[6]);

                    Member member = membersDb.getOrDefault(
                            memberId, new Member(memberId, name, nationalId, pin));
                    membersDb.put(memberId, member);
                    registeredNationalIds.add(nationalId);

                    if (!accType.equals("NONE")) {
                        Account account = accType.equals("Savings")
                                ? new SavingsAccount(memberId, accNumber, balance)
                                : new LoanAccount(memberId, accNumber, balance);
                        member.addAccount(accType, account);
                    }
                }
            }
            System.out.println("Loaded " + membersDb.size() + " member(s) from " + MEMBERS_FILE + ".");
        } catch (FileNotFoundException | NumberFormatException e) {
            System.out.println("Error loading data from " + MEMBERS_FILE + ": " + e.getMessage());
        }
    }

    private static void saveData() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(MEMBERS_FILE))) {
            for (Member member : membersDb.values()) {
                if (member.getAccounts().isEmpty()) {
                    // Persist members even if they haven't opened an account yet
                    pw.println(String.join(",",
                            member.getMemberId(),
                            member.getName(),
                            member.getNationalId(),
                            member.getPin(),
                            "NONE",
                            "NONE",
                            "0.0"));
                } else {
                    for (Map.Entry<String, Account> entry : member.getAccounts().entrySet()) {
                        Account acc = entry.getValue();
                        pw.println(String.join(",",
                                member.getMemberId(),
                                member.getName(),
                                member.getNationalId(),
                                member.getPin(),
                                entry.getKey(),
                                acc.getAccountNumber(),
                                String.valueOf(acc.getBalance())));
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Error saving data to " + MEMBERS_FILE + ": " + e.getMessage());
        }
    }
}