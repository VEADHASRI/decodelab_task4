import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.InputMismatchException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * Project 4 — Currency Converter
 * DecodeLabs Industrial Training Kit | Java Programming
 *
 * Goal: convert an amount from one currency to another, supporting
 * multiple currencies via a USD pivot for cross-rate conversion.
 *
 * Architecture (IPO Model):
 *   Intake     : Scanner + do-while menu + switch routing + the Security Gate
 *   Combustion : BigDecimal arithmetic, USD as the pivot currency
 *   Exhaust    : printf with thousands separators and fixed 2-decimal precision
 *
 * Why BigDecimal instead of double: money is not a number, it's sensitive
 * data. double is IEEE-754 binary floating point — it cannot exactly
 * represent most base-10 decimals (0.1 + 0.2 == 0.30000000000000004).
 * BigDecimal gives arbitrary, exact precision, which is the enterprise
 * standard for banking, invoicing, and currency conversion.
 */
public class decodelabs_java_P4{

    // Predefined exchange rates: units of currency per 1 USD (USD is the pivot).
    // Using String constructors for BigDecimal is mandatory — BigDecimal(double)
    // would inherit the same floating-point imprecision we are trying to avoid.
    private static final Map<String, BigDecimal> RATES = buildRates();

    private static Map<String, BigDecimal> buildRates() {
        Map<String, BigDecimal> rates = new LinkedHashMap<>();
        rates.put("USD", new BigDecimal("1.00"));
        rates.put("KWD", new BigDecimal("0.31"));
        rates.put("EUR", new BigDecimal("0.92"));
        rates.put("INR", new BigDecimal("83.50"));
        rates.put("GBP", new BigDecimal("0.79"));
        rates.put("JPY", new BigDecimal("151.50"));
        rates.put("AUD", new BigDecimal("1.52"));
        rates.put("CAD", new BigDecimal("1.36"));
        return rates;
    }

    // Indexed view of the currency codes, built once, used for menu display
    // and number-based selection.
    private static final String[] CURRENCY_CODES = RATES.keySet().toArray(new String[0]);

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        boolean running = true;

        System.out.println("===== Currency Converter =====");

        // The do-while Loop: the menu body must run at least once, and lets
        // the user perform multiple conversions without restarting the app.
        do {
            printMainMenu();
            int choice = readIntInRange(sc, "Select an option: ", 1, 2);

            // The switch Statement: clean, direct routing for menu options.
            // Always includes a break (no fall-through) and a default case
            // (traps invalid choices, even though readIntInRange already
            // constrains the range — defensive programming, belt and braces).
            switch (choice) {
                case 1:
                    performConversion(sc);
                    break;
                case 2:
                    running = false;
                    System.out.println("Exiting Currency Converter. Goodbye!");
                    break;
                default:
                    System.out.println("Unrecognized option. Returning to menu.");
                    break;
            }
        } while (running);

        sc.close();
    }

    private static void printMainMenu() {
        System.out.println();
        System.out.println("1. Convert Currency");
        System.out.println("2. Exit");
    }

    private static void printCurrencyMenu() {
        System.out.println();
        System.out.println("Available Currencies:");
        for (int i = 0; i < CURRENCY_CODES.length; i++) {
            System.out.printf("  %d. %s%n", (i + 1), CURRENCY_CODES[i]);
        }
    }

    /**
     * The Intake: capture source currency, target currency, and amount.
     * The Security Gate: validate the logic of the money before the
     * engine ever touches it — invalid input never reaches the calculation.
     */
    private static void performConversion(Scanner sc) {
        printCurrencyMenu();

        int sourceIndex = readIntInRange(sc, "Select source currency (number): ", 1, CURRENCY_CODES.length);
        int targetIndex = readIntInRange(sc, "Select target currency (number): ", 1, CURRENCY_CODES.length);

        String sourceCode = CURRENCY_CODES[sourceIndex - 1];
        String targetCode = CURRENCY_CODES[targetIndex - 1];

        BigDecimal amount = readPositiveAmount(sc, "Enter amount in " + sourceCode + ": ");

        BigDecimal converted = convert(amount, sourceCode, targetCode);

        displayResult(amount, sourceCode, converted, targetCode);
    }

    /**
     * The Combustion Engine: cross-rate conversion routed through USD.
     * Direct conversion would require hardcoding every possible currency
     * pair; pivoting through USD lets N currencies support N*(N-1) pairs
     * with only N rates stored.
     *
     * Step 1: amount -> USD            (amount / sourceRate)
     * Step 2: USD    -> target currency (usdAmount * targetRate)
     */
    private static BigDecimal convert(BigDecimal amount, String sourceCode, String targetCode) {
        BigDecimal sourceRate = RATES.get(sourceCode);
        BigDecimal targetRate = RATES.get(targetCode);

        // Intermediate division kept at high scale (10 dp) so precision isn't
        // lost before the final multiplication — only the FINAL output gets
        // rounded down to 2 decimal places.
        BigDecimal amountInUsd = amount.divide(sourceRate, 10, RoundingMode.HALF_EVEN);

        // Round Half Even (Banker's Rounding) is the enterprise standard:
        // it eliminates the cumulative rounding bias that Round Half Up
        // introduces across millions of transactions.
        return amountInUsd.multiply(targetRate).setScale(2, RoundingMode.HALF_EVEN);
    }

    /**
     * The Exhaust: financial polish. Raw output exposes floating-point
     * messiness; this enforces strict two-decimal-place formatting with
     * thousands-separator grouping, building user trust in the result.
     */
    private static void displayResult(BigDecimal amount, String sourceCode,
                                       BigDecimal converted, String targetCode) {
        System.out.println();
        System.out.println("---------- Conversion Result ----------");
        System.out.printf("%,.2f %s = %,.2f %s%n", amount, sourceCode, converted, targetCode);
        System.out.println("----------------------------------------");
    }

    /**
     * Reads an integer menu choice, defending against the classic Scanner
     * buffer trap: if nextInt() throws InputMismatchException, the bad
     * token is still stuck in the buffer. The fix is to call nextLine()
     * inside the catch block to clear it before re-prompting — otherwise
     * the loop re-reads the same stuck token forever.
     */
    private static int readIntInRange(Scanner sc, String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            try {
                int value = sc.nextInt();
                sc.nextLine(); // consume the leftover newline left in the buffer
                if (value < min || value > max) {
                    System.out.println("  -> Invalid: enter a number between " + min + " and " + max + ".");
                    continue;
                }
                return value;
            } catch (InputMismatchException e) {
                sc.nextLine(); // clear the invalid token so it isn't read again
                System.out.println("  -> Invalid: please enter a whole number.");
            }
        }
    }

    /**
     * Reads a monetary amount as a BigDecimal (never a double — see class
     * header). The Security Gate rule: reject negative amounts immediately,
     * display an error, and route the user back rather than crashing the
     * system or outputting nonsensical data.
     */
    private static BigDecimal readPositiveAmount(Scanner sc, String prompt) {
        while (true) {
            System.out.print(prompt);
            String token;
            try {
                token = sc.next();
            } finally {
                sc.nextLine(); // always clear the rest of the line
            }

            try {
                BigDecimal amount = new BigDecimal(token);
                if (amount.compareTo(BigDecimal.ZERO) < 0) {
                    System.out.println("  -> Invalid: amount cannot be negative.");
                    continue;
                }
                return amount;
            } catch (NumberFormatException e) {
                System.out.println("  -> Invalid: please enter a valid numeric amount.");
            }
        }
    }
}
