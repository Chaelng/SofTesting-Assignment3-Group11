package stepdefinitions;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class TestContext {

    public static WebDriver driver;
    public static Properties testData = loadTestData();

    public static String firstAccountId;
    public static String secondAccountId;
    public static Map<String, BigDecimal> balancesBefore = new LinkedHashMap<>();
    public static int transactionCountBefore;

    // Clears the values remembered from the previous scenario
    public static void reset() {
        firstAccountId = null;
        secondAccountId = null;
        balancesBefore = new LinkedHashMap<>();
        transactionCountBefore = 0;
    }

    // Reads one value from testdata.properties
    public static String data(String key) {
        return testData.getProperty(key);
    }

    // Loads the customer and payee details from testdata.properties
    private static Properties loadTestData() {
        Properties properties = new Properties();
        try (InputStream input = TestContext.class.getClassLoader().getResourceAsStream("testdata.properties")) {
            properties.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Could not load testdata.properties", e);
        }
        return properties;
    }

    // Opens Accounts Overview and returns every account number with its balance
    public static Map<String, BigDecimal> readBalances() throws InterruptedException {
        driver.findElement(By.linkText("Accounts Overview")).click();
        Thread.sleep(3000);

        Map<String, BigDecimal> balances = new LinkedHashMap<>();
        List<WebElement> rows = driver.findElements(By.xpath("//table[@id='accountTable']/tbody/tr[td/a]"));
        for (WebElement row : rows) {
            String accountId = row.findElement(By.xpath("./td[1]")).getText();
            String balance = row.findElement(By.xpath("./td[2]")).getText();
            balances.put(accountId, toMoney(balance));
        }
        return balances;
    }

    // Opens the account activity page and counts the transactions listed
    public static int countTransactions(String accountId) throws InterruptedException {
        driver.get(data("parabank.baseUrl") + "/activity.htm?id=" + accountId);
        Thread.sleep(3000);
        return driver.findElements(By.cssSelector("#transactionTable tbody tr")).size();
    }

    // Remembers the balances and transaction count before the action under test
    public static void rememberAccountState() throws InterruptedException {
        balancesBefore = readBalances();
        transactionCountBefore = countTransactions(firstAccountId);
    }

    // Converts text such as "$1,515.50" or "-$100.00" to a number
    public static BigDecimal toMoney(String text) {
        String cleaned = text.replace("$", "").replace(",", "").trim();
        return new BigDecimal(cleaned);
    }

    // Returns all visible text on the current page
    public static String pageText() {
        return driver.findElement(By.tagName("body")).getText();
    }
}
