package tests;

import base.BaseTest;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import pages.LoginPage;
import pages.RegistrationPage;
import utils.TestData;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BillPayTest extends BaseTest {

    @Test
    public void customerCanPayBill() {
        LoginPage loginPage = createAndLoginCustomer();
        driver.findElement(By.linkText("Bill Pay")).click();

        By sourceAccount = By.name("fromAccountId");
        wait.until(ExpectedConditions.visibilityOfElementLocated(sourceAccount));
        new Select(driver.findElement(sourceAccount)).selectByIndex(0);

        driver.findElement(By.name("payee.name")).sendKeys("Test Utility");
        driver.findElement(By.name("payee.address.street")).sendKeys("1 Test Street");
        driver.findElement(By.name("payee.address.city")).sendKeys("Melbourne");
        driver.findElement(By.name("payee.address.state")).sendKeys("VIC");
        driver.findElement(By.name("payee.address.zipCode")).sendKeys("3000");
        driver.findElement(By.name("payee.phoneNumber")).sendKeys("0400000000");
        driver.findElement(By.name("payee.accountNumber")).sendKeys("123456789");
        driver.findElement(By.name("verifyAccount")).sendKeys("123456789");
        driver.findElement(By.name("amount")).sendKeys("10.00");
        driver.findElement(By.cssSelector("input[value='Send Payment']")).click();

        wait.until(ExpectedConditions.textToBePresentInElementLocated(
                By.id("rightPanel"), "Bill Payment Complete"));
        String result = loginPage.getResultText();
        assertTrue(result.contains("Bill Payment Complete"),
                "ParaBank should confirm that the bill payment completed");
        assertTrue(result.contains("Test Utility") && result.contains("$10.00"),
                "The confirmation should identify the payee and payment amount");
    }

    @Test
    public void billPayRejectsMissingAmount() {
        createAndLoginCustomer();
        driver.findElement(By.linkText("Bill Pay")).click();

        By sourceAccount = By.name("fromAccountId");
        wait.until(ExpectedConditions.visibilityOfElementLocated(sourceAccount));
        new Select(driver.findElement(sourceAccount)).selectByIndex(0);

        driver.findElement(By.name("payee.name")).sendKeys("Test Utility");
        driver.findElement(By.name("payee.address.street")).sendKeys("1 Test Street");
        driver.findElement(By.name("payee.address.city")).sendKeys("Melbourne");
        driver.findElement(By.name("payee.address.state")).sendKeys("VIC");
        driver.findElement(By.name("payee.address.zipCode")).sendKeys("3000");
        driver.findElement(By.name("payee.phoneNumber")).sendKeys("0400000000");
        driver.findElement(By.name("payee.accountNumber")).sendKeys("123456789");
        driver.findElement(By.name("verifyAccount")).sendKeys("123456789");
        // Deliberately leave the amount blank to exercise client-side validation.
        driver.findElement(By.cssSelector("input[value='Send Payment']")).click();

        By amountRequired = By.id("validationModel-amount-empty");
        wait.until(ExpectedConditions.visibilityOfElementLocated(amountRequired));
        assertTrue(!driver.findElement(amountRequired).getText().isBlank(),
                "ParaBank should show a validation message for the missing amount");
        assertTrue(driver.findElement(By.id("billpayForm")).isDisplayed(),
                "The bill payment form should remain visible after validation fails");
        assertFalse(driver.findElement(By.id("billpayResult")).isDisplayed(),
                "A successful payment result should not be displayed");
    }

    @Test
    public void billPayRejectsMissingPayee() {
        createAndLoginCustomer();
        driver.findElement(By.linkText("Bill Pay")).click();

        By sourceAccount = By.name("fromAccountId");
        wait.until(ExpectedConditions.visibilityOfElementLocated(sourceAccount));
        new Select(driver.findElement(sourceAccount)).selectByIndex(0);

        // Leave only the payee name blank to exercise its required-field validation.
        driver.findElement(By.name("payee.address.street")).sendKeys("1 Test Street");
        driver.findElement(By.name("payee.address.city")).sendKeys("Melbourne");
        driver.findElement(By.name("payee.address.state")).sendKeys("VIC");
        driver.findElement(By.name("payee.address.zipCode")).sendKeys("3000");
        driver.findElement(By.name("payee.phoneNumber")).sendKeys("0400000000");
        driver.findElement(By.name("payee.accountNumber")).sendKeys("123456789");
        driver.findElement(By.name("verifyAccount")).sendKeys("123456789");
        driver.findElement(By.name("amount")).sendKeys("10.00");
        driver.findElement(By.cssSelector("input[value='Send Payment']")).click();

        By payeeRequired = By.id("validationModel-name");
        wait.until(ExpectedConditions.visibilityOfElementLocated(payeeRequired));
        assertFalse(driver.findElement(payeeRequired).getText().isBlank(),
                "ParaBank should show a validation message for the missing payee name");
        assertTrue(driver.findElement(By.id("billpayForm")).isDisplayed(),
                "The bill payment form should remain visible after validation fails");
        assertFalse(driver.findElement(By.id("billpayResult")).isDisplayed(),
                "A successful payment result should not be displayed");
    }

    private LoginPage createAndLoginCustomer() {
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
        return loginPage;
    }
}
