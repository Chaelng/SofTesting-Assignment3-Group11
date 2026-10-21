package tests;

import base.BaseTest;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import pages.LoginPage;
import pages.RegistrationPage;
import utils.TestData;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FindTransactionsTest extends BaseTest {

    /**
     * Spira Test Case: TC:55597
     */
    @Test
    @Order(1)
    public void findTransactionById() {
        String payeeName = uniquePayeeName();
        String paymentAmount = "37.41";
        String sourceAccountId = createBillPayment(payeeName, paymentAmount);

        searchTransactionsByAmount(sourceAccountId, paymentAmount);
        String transactionId = getFirstTransactionId();

        selectSearchAccount(sourceAccountId);
        driver.findElement(By.id("transactionId")).sendKeys(transactionId);
        driver.findElement(By.id("findById")).click();

        WebElement results = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.id("transactionTable")));
        String resultText = results.getText();
        assertTrue(resultText.contains(payeeName),
                "Searching by transaction ID should return the created bill payment");
        assertTrue(resultText.contains(paymentAmount),
                "The matching transaction should display the payment amount");
    }

    /**
     * Spira Test Case: TC:55599
     */
    @Test
    @Order(2)
    public void findTransactionByAmount() {
        String payeeName = uniquePayeeName();
        String paymentAmount = "37.41";
        String sourceAccountId = createBillPayment(payeeName, paymentAmount);

        WebElement results = searchTransactionsByAmount(sourceAccountId, paymentAmount);
        String resultText = results.getText();
        assertTrue(resultText.contains(payeeName),
                "Searching by amount should return the created bill payment");
        assertTrue(resultText.contains(paymentAmount),
                "The matching transaction should display the searched amount");
    }

    /**
     * Spira Test Case: TC:55600
     */
    @Test
    @Order(3)
    public void noResultingTransactionFromCriteria() {
        String payeeName = uniquePayeeName();
        String sourceAccountId = createBillPayment(payeeName, "37.41");

        selectSearchAccount(sourceAccountId);
        driver.findElement(By.id("transactionId")).sendKeys("999999999");
        driver.findElement(By.id("findById")).click();

        String pageText = driver.findElement(By.tagName("body")).getText();
        assertFalse(pageText.contains(payeeName),
                "An unmatched transaction ID should not display the created bill payment");
        assertTrue(driver.findElements(By.cssSelector("#transactionTable a[href*='id=']"))
                        .stream().noneMatch(WebElement::isDisplayed),
                "An unmatched transaction ID should not display transaction results");
    }

    private String createBillPayment(String payeeName, String paymentAmount) {
        String sourceAccountId = createAndLoginCustomerAndOpenBillPay();

        driver.findElement(By.name("payee.name")).sendKeys(payeeName);
        driver.findElement(By.name("payee.address.street")).sendKeys("1 Test Street");
        driver.findElement(By.name("payee.address.city")).sendKeys("Melbourne");
        driver.findElement(By.name("payee.address.state")).sendKeys("VIC");
        driver.findElement(By.name("payee.address.zipCode")).sendKeys("3000");
        driver.findElement(By.name("payee.phoneNumber")).sendKeys("0400000000");
        driver.findElement(By.name("payee.accountNumber")).sendKeys("123456789");
        driver.findElement(By.name("verifyAccount")).sendKeys("123456789");
        driver.findElement(By.name("amount")).sendKeys(paymentAmount);
        driver.findElement(By.cssSelector("input[value='Send Payment']")).click();

        By rightPanel = By.id("rightPanel");
        wait.until(ExpectedConditions.textToBePresentInElementLocated(
                rightPanel, "Bill Payment Complete"));
        String confirmation = driver.findElement(rightPanel).getText();
        assertTrue(confirmation.contains(payeeName),
                "ParaBank should confirm the bill payment to the test payee");
        assertTrue(confirmation.contains(paymentAmount),
                "ParaBank should confirm the bill payment amount");
        return sourceAccountId;
    }

    private String createAndLoginCustomerAndOpenBillPay() {
        String username = TestData.uniqueUsername();
        RegistrationPage registrationPage = new RegistrationPage(driver, wait);
        registrationPage.openRegistration();
        registrationPage.registerNewUser(username, TestData.PASSWORD);

        wait.until(ExpectedConditions.textToBePresentInElementLocated(
                By.id("rightPanel"), "Your account was created successfully"));

        LoginPage loginPage = new LoginPage(driver, wait);
        loginPage.logout();
        loginPage.login(username, TestData.PASSWORD);
        wait.until(ExpectedConditions.textToBePresentInElementLocated(
                By.id("rightPanel"), "Accounts Overview"));

        driver.findElement(By.linkText("Bill Pay")).click();
        By sourceAccount = By.name("fromAccountId");
        wait.until(driver -> !new Select(driver.findElement(sourceAccount)).getOptions().isEmpty());
        return new Select(driver.findElement(sourceAccount))
                .getFirstSelectedOption()
                .getAttribute("value");
    }

    private WebElement searchTransactionsByAmount(String sourceAccountId, String paymentAmount) {
        selectSearchAccount(sourceAccountId);
        driver.findElement(By.id("amount")).sendKeys(paymentAmount);
        driver.findElement(By.id("findByAmount")).click();

        return wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.id("transactionTable")));
    }

    private void selectSearchAccount(String sourceAccountId) {
        driver.findElement(By.linkText("Find Transactions")).click();
        By searchAccount = By.id("accountId");
        wait.until(ExpectedConditions.visibilityOfElementLocated(searchAccount));
        new Select(driver.findElement(searchAccount)).selectByValue(sourceAccountId);
    }

    private String getFirstTransactionId() {
        By transactionLink = By.cssSelector("#transactionTable a[href*='id=']");
        String href = wait.until(
                ExpectedConditions.presenceOfElementLocated(transactionLink))
                .getAttribute("href");
        String query = URI.create(href).getQuery();
        if (query == null) {
            throw new IllegalStateException("Transaction link does not contain an ID query parameter");
        }

        for (String parameter : query.split("&")) {
            if (parameter.startsWith("id=")) {
                return parameter.substring("id=".length());
            }
        }
        throw new IllegalStateException("Transaction link does not contain an ID query parameter");
    }

    private String uniquePayeeName() {
        return "Test Utility";
    }
}
