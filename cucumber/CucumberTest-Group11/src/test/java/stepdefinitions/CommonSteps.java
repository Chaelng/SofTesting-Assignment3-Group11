package stepdefinitions;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static stepdefinitions.TestContext.data;

public class CommonSteps {

    // Registers a brand-new customer so every scenario starts with a fresh $515.50 account
    @Given("I am logged in as a registered customer")
    public void iAmLoggedInAsARegisteredCustomer() throws InterruptedException {
        WebDriver driver = TestContext.driver;
        driver.get(data("parabank.baseUrl") + "/index.htm");
        driver.findElement(By.linkText("Register")).click();
        Thread.sleep(2000);

        String username = data("customer.usernamePrefix") + String.valueOf(System.currentTimeMillis()).substring(4);
        driver.findElement(By.id("customer.firstName")).sendKeys(data("customer.firstName"));
        driver.findElement(By.id("customer.lastName")).sendKeys(data("customer.lastName"));
        driver.findElement(By.id("customer.address.street")).sendKeys(data("customer.address"));
        driver.findElement(By.id("customer.address.city")).sendKeys(data("customer.city"));
        driver.findElement(By.id("customer.address.state")).sendKeys(data("customer.state"));
        driver.findElement(By.id("customer.address.zipCode")).sendKeys(data("customer.zipCode"));
        driver.findElement(By.id("customer.phoneNumber")).sendKeys(data("customer.phone"));
        driver.findElement(By.id("customer.ssn")).sendKeys(data("customer.ssn"));
        driver.findElement(By.id("customer.username")).sendKeys(username);
        driver.findElement(By.id("customer.password")).sendKeys(data("customer.password"));
        driver.findElement(By.id("repeatedPassword")).sendKeys(data("customer.password"));
        driver.findElement(By.cssSelector("input[value='Register']")).click();
        Thread.sleep(3000);

        assertTrue(TestContext.pageText().contains("Your account was created successfully"),
                "Registration of a new customer failed");

        Map<String, BigDecimal> balances = TestContext.readBalances();
        TestContext.firstAccountId = balances.keySet().iterator().next();
        TestContext.rememberAccountState();
    }

    // Checks that the given message is visible on the page
    @Then("I should see the message {string}")
    public void iShouldSeeTheMessage(String message) throws InterruptedException {
        Thread.sleep(2000);
        assertTrue(TestContext.pageText().contains(message),
                "Expected message not shown: " + message);
    }

    // Checks that at least one error message is visible on the page
    @Then("I should see an error message")
    public void iShouldSeeAnErrorMessage() throws InterruptedException {
        Thread.sleep(2000);
        List<WebElement> errors = TestContext.driver.findElements(By.cssSelector(".error"));
        boolean errorShown = errors.stream().anyMatch(WebElement::isDisplayed);
        assertTrue(errorShown, "No error message was shown");
    }

    // Checks that the first account balance went down by the given amount
    @Then("my first account balance should decrease by ${bigdecimal}")
    public void myFirstAccountBalanceShouldDecreaseBy(BigDecimal amount) throws InterruptedException {
        Map<String, BigDecimal> balancesAfter = TestContext.readBalances();
        BigDecimal before = TestContext.balancesBefore.get(TestContext.firstAccountId);
        BigDecimal after = balancesAfter.get(TestContext.firstAccountId);
        assertEquals(0, before.subtract(amount).compareTo(after),
                "First account balance: expected " + before.subtract(amount) + " but was " + after);
    }

    // Checks that no balance changed and no new account was created
    @Then("my account balances should not change")
    public void myAccountBalancesShouldNotChange() throws InterruptedException {
        Map<String, BigDecimal> balancesAfter = TestContext.readBalances();
        assertEquals(TestContext.balancesBefore.keySet(), balancesAfter.keySet(),
                "The list of accounts changed");
        for (String accountId : TestContext.balancesBefore.keySet()) {
            assertEquals(0, TestContext.balancesBefore.get(accountId).compareTo(balancesAfter.get(accountId)),
                    "Balance of account " + accountId + " changed");
        }
    }

    // Checks that the first account's activity has no extra transactions
    @Then("no new transaction should appear in my account history")
    public void noNewTransactionShouldAppearInMyAccountHistory() throws InterruptedException {
        int transactionCountAfter = TestContext.countTransactions(TestContext.firstAccountId);
        assertEquals(TestContext.transactionCountBefore, transactionCountAfter,
                "A new transaction was recorded");
    }
}
