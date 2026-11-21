import java.io.*;
import java.util.*;
// ============================================================
// 1. CUSTOM EXCEPTION
// ============================================================
// We create a custom exception to handle business rules.
// Extending 'Exception' makes it a checked exception, meaning
// the compiler forces the programmer to handle it.
class InsufficientFundsException extends Exception {
 public InsufficientFundsException(String message) {
 super(message);
 }
}
// ============================================================
// 2. INTERFACE
// ============================================================
// An interface is a contract. Any class that implements this MUST
// provide a logTransaction method.
interface Auditable {
 void logTransaction(String message);
}
// ============================================================
// 3. ABSTRACT CLASS (PARENT)
// ============================================================
// Account is abstract because a generic "Account" doesn't exist in reality.
// It must be either a Savings or Loan account.
abstract class Account implements Auditable {
 // Encapsulation: Fields are private so they can't be changed directly.
 private String accountNumber;
 private double balance;
 // Collection: ArrayList to store transaction history in memory.
 private ArrayList<String> transactionHistory;
 public Account(String accountNumber, double initialBalance) {
 this.accountNumber = accountNumber;
 this.balance = initialBalance;
 this.transactionHistory = new ArrayList<>();
 }
 // Getters
 public String getAccountNumber() { return accountNumber; }
 public double getBalance() { return balance; }
 public ArrayList<String> getTransactionHistory() { return transactionHistory; }
 // Business Logic: Deposit
 public void deposit(double amount) {
 if (amount > 0) {
 balance += amount;
 String log = "Deposited: " + amount + " | New Balance: " + balance;
 transactionHistory.add(log);
 logTransaction(log); // Calling the interface method
 } else {
 System.out.println("Error: Deposit amount must be positive.");
 }
 }
 // Business Logic: Withdraw (Throws Custom Exception)
 public void withdraw(double amount) throws InsufficientFundsException {
 if (amount <= 0) {
 System.out.println("Error: Withdrawal amount must be positive.");
 return;
 }
 if (amount > balance) {
 // Firing the custom exception
 throw new InsufficientFundsException("Insufficient funds. Balance is: " +
balance);
 }
 balance -= amount;
 String log = "Withdrew: " + amount + " | New Balance: " + balance;
 transactionHistory.add(log);
 logTransaction(log);
 }
// Abstract Method: Forces subclasses to implement their own interest logic.
 public abstract double calculateInterest();
 // Implementing the Auditable interface. We write to a text file.
 @Override
 public void logTransaction(String message) {
 // try-with-resources automatically closes the file to prevent memory leaks.
 try (FileWriter fw = new FileWriter(SaccoManager.TRANSACTIONS_FILE, true);
 PrintWriter pw = new PrintWriter(fw)) {
 pw.println(accountNumber + " | " + message);
 } catch (IOException e) {
 System.out.println("Error writing to transaction log: " + e.getMessage())
 ;
 }
 }
}
// ============================================================
// 4. CONCRETE SUBCLASSES
// ============================================================
class SavingsAccount extends Account {
 public SavingsAccount(String accountNumber, double initialBalance) {
 super(accountNumber, initialBalance);
 }
 // Polymorphism: Overriding the abstract method.
 @Override
 public double calculateInterest() {
 return getBalance() * 0.05; // 5% interest
 }
}
class LoanAccount extends Account {
 public LoanAccount(String accountNumber, double initialBalance) {
 super(accountNumber, initialBalance);
 }
 @Override
 public double calculateInterest() {
 return getBalance() * 0.12; // 12% interest
 }
}
// ============================================================
// 5. DOMAIN CLASS (MEMBER)
// ============================================================
class Member {
 private String memberId;
 private String name;
 private String nationalId;
 // A member can have multiple accounts. We use a Map to store them by account type (e.g., "Savings", "Loan").
 private Map<String, Account> accounts;
 public Member(String memberId, String name, String nationalId) {
 this.memberId = memberId;
 this.name = name;
 this.nationalId = nationalId;
 this.accounts = new HashMap<>();
 }
 public String getMemberId() { return memberId; }
 public String getName() { return name; }
 public String getNationalId() { return nationalId; }
 public Map<String, Account> getAccounts() { return accounts; }
 public void addAccount(String type, Account account) {
 accounts.put(type, account);
 }
}
// ============================================================
// 6. MAIN APPLICATION CLASS
// ============================================================
public class SaccoManager {
 // We use a HashMap to store members by their unique Member ID for O(1) lookup.
 private static HashMap<String, Member> membersDb = new HashMap<>();
 // We use a HashSet to guarantee National IDs are unique.
 private static HashSet<String> registeredNationalIds = new HashSet<>();
 private static final String MEMBERS_FILE = "members.csv";
 static final String TRANSACTIONS_FILE = "transactions.txt";
 public static void main(String[] args) {
 Scanner scanner = new Scanner(System.in);

 // Step 1: Load existing data from file
 loadData();
 int choice;
 do {
 System.out.println("\n=== M-KOPO SACCO MANAGEMENT SYSTEM ===");
 System.out.println("1. Register New Member");
 System.out.println("2. Open an Account");
 System.out.println("3. Deposit Money");
 System.out.println("4. Withdraw Money");
 System.out.println("5. View Member Details");
 System.out.println("6. Exit & Save");
 System.out.print("Enter choice: ");
 // Error handling for bad input (e.g., typing letters instead of numbers)
 while (!scanner.hasNextInt()) {
 System.out.println("Invalid input. Please enter a number.");
 scanner.next();
 }
 choice = scanner.nextInt();
 scanner.nextLine(); // Consume the newline character
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
 saveData();
 System.out.println("Exiting system. Data saved successfully. Goodbye!");
 break;
 default:
 System.out.println("Invalid choice. Try again.");
 }
 } while (choice != 6);
 scanner.close();
 }
 // ============================================================
 // APPLICATION METHODS
 // ============================================================
 private static void registerMember(Scanner scanner) {
 System.out.print("Enter Member ID (e.g., M-001): ");
 String id = scanner.nextLine();
 if (membersDb.containsKey(id)) {
 System.out.println("Error: Member ID already exists.");
 return;
 }
 System.out.print("Enter Full Name: ");
 String name = scanner.nextLine();
 System.out.print("Enter National ID: ");
 String nationalId = scanner.nextLine();
 // Validate uniqueness using the HashSet
 if (registeredNationalIds.contains(nationalId)) {
 System.out.println("Error: A member with this National ID already exists.");
 return;
 }
 Member newMember = new Member(id, name, nationalId);
 membersDb.put(id, newMember);
 registeredNationalIds.add(nationalId);
 System.out.println("Success: Member registered successfully!");
 }
 private static void openAccount(Scanner scanner) {
 System.out.print("Enter Member ID: ");
 String id = scanner.nextLine();
 Member member = membersDb.get(id);
 if (member == null) {
 System.out.println("Error: Member not found.");
 return;
 }
 System.out.println("1. Savings Account");
 System.out.println("2. Loan Account");
 System.out.print("Choose account type: ");
 int type = scanner.nextInt();
 scanner.nextLine(); // Consume newline
 // Generate a unique account number using the Member ID and the current time
 String accountNumber = id + "-" + System.currentTimeMillis() % 10000;
 if (type == 1) {
 member.addAccount("Savings", new SavingsAccount(accountNumber, 0));
 System.out.println("Success: Savings Account (" + accountNumber + ") opened.");
 } else if (type == 2) {
 member.addAccount("Loan", new LoanAccount(accountNumber, 0));
 System.out.println("Success: Loan Account (" + accountNumber + ") opened.");
 } else {
 System.out.println("Invalid account type.");
 }
 }
 private static void depositMoney(Scanner scanner) {
 Account account = findAccount(scanner);
 if (account == null) return;
 System.out.print("Enter amount to deposit: ");
 double amount = scanner.nextDouble();
 account.deposit(amount);
 System.out.println("New Balance: " + account.getBalance());
 }
 private static void withdrawMoney(Scanner scanner) {
 Account account = findAccount(scanner);
 if (account == null) return;
 System.out.print("Enter amount to withdraw: ");
 double amount = scanner.nextDouble();
 try {
 // We MUST use try-catch because withdraw() throws a Checked Exception
 account.withdraw(amount);
 System.out.println("New Balance: " + account.getBalance());
 } catch (InsufficientFundsException e) {
 // Catching our custom exception
 System.out.println("TRANSACTION FAILED: " + e.getMessage());
 }
 }
 private static void viewMemberDetails(Scanner scanner) {
 System.out.print("Enter Member ID: ");
 String id = scanner.nextLine();
 Member member = membersDb.get(id);
 if (member == null) {
 System.out.println("Error: Member not found.");
 return;
 }
 System.out.println("\n--- MEMBER DETAILS ---");
 System.out.println("Name: " + member.getName());
 System.out.println("Member ID: " + member.getMemberId());
 System.out.println("National ID: " + member.getNationalId());

 Map<String, Account> accounts = member.getAccounts();
 if (accounts.isEmpty()) {
 System.out.println("No accounts opened yet.");
 } else {
 for (Map.Entry<String, Account> entry : accounts.entrySet()) {
 Account acc = entry.getValue();
 System.out.println("Account: " + entry.getKey() + " | No: " + acc.getAccountNumber() +
 " | Balance: KSh " + acc.getBalance());
 // Polymorphism in action! calculateInterest() acts differently depending on the object.
 System.out.println(" Potential Interest: KSh " + acc.calculateInterest());
 }
 }
 }
 // Helper method to find an account
 private static Account findAccount(Scanner scanner) {
 System.out.print("Enter Member ID: ");
 String id = scanner.nextLine();
 Member member = membersDb.get(id);
 if (member == null) {
 System.out.println("Error: Member not found.");
 return null;
 }
 System.out.print("Enter account type (Savings/Loan): ");
 String type = scanner.nextLine();
 Account account = member.getAccounts().get(type);
 if (account == null) {
 System.out.println("Error: Account type not found for this member.");
 return null;
 }
 return account;
 }
 // ============================================================
 // FILE I/O METHODS
 // ============================================================
 // Load data when the program starts
 private static void loadData() {
 File file = new File(MEMBERS_FILE);
 if (!file.exists()) {
 System.out.println("No previous data found. Starting fresh.");
 return;
 }
 try (Scanner fileScanner = new Scanner(file)) {
 while (fileScanner.hasNextLine()) {
 String line = fileScanner.nextLine();
 String[] parts = line.split(",");

 // Format: MemberId,Name,NationalId,AccountType,AccountNumber,Balance
 if (parts.length == 6) {
 String memberId = parts[0];
 String name = parts[1];
 String nationalId = parts[2];
 String accType = parts[3];
 String accNumber = parts[4];
 double balance = Double.parseDouble(parts[5]);
 // Reconstruct Member
 Member member = membersDb.getOrDefault(memberId, new Member(memberId, name, nationalId));
 membersDb.put(memberId, member);
 registeredNationalIds.add(nationalId);
 // Reconstruct Account
 Account account;
 if (accType.equals("Savings")) {
 account = new SavingsAccount(accNumber, balance);
 } else {
 account = new LoanAccount(accNumber, balance);
 }
 member.addAccount(accType, account);
 }
 }
 System.out.println("Data loaded successfully from " + MEMBERS_FILE);
 } catch (FileNotFoundException e) {
 System.out.println("Error loading data: " + e.getMessage());
 }
 }
 // Save data when the program exits
 private static void saveData() {
 try (PrintWriter pw = new PrintWriter(new FileWriter(MEMBERS_FILE))) {
 // Loop through all members and their accounts
 for (Member member : membersDb.values()) {
 for (Map.Entry<String, Account> entry : member.getAccounts().entrySet
()) {
 String accType = entry.getKey();
 Account acc = entry.getValue();

 // Write to CSV: MemberId,Name,NationalId,AccountType,AccountNumber,Balance
 pw.println(member.getMemberId() + "," +
 member.getName() + "," +
 member.getNationalId() + "," +
 accType + "," +
 acc.getAccountNumber() + "," +
 acc.getBalance());
 }
 }
  } catch (IOException e) {
 System.out.println("Error saving data: " + e.getMessage());
 }
 }
}
