package tests;

import base.BaseTest;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import pages.LoginPage;
import pages.RegistrationPage;
import utils.TestData;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OpenNewAccountTest extends BaseTest {

    @Test
    public void customerCanOpenCheckingAccount() {
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
        driver.findElement(By.linkText("Open New Account")).click();

        By accountType = By.id("type");
        wait.until(ExpectedConditions.visibilityOfElementLocated(accountType));
        new Select(driver.findElement(accountType)).selectByVisibleText("CHECKING");
        By sourceAccount = By.id("fromAccountId");
        wait.until(driver -> !new Select(driver.findElement(sourceAccount)).getOptions().isEmpty());
        driver.findElement(By.cssSelector("input[value='Open New Account']")).click();

        wait.until(ExpectedConditions.textToBePresentInElementLocated(
                By.id("rightPanel"), "Congratulations, your account is now open."));
        assertTrue(driver.findElement(By.id("rightPanel")).getText()
                        .contains("Congratulations, your account is now open."),
                "ParaBank should confirm that the new account was opened");
        By newAccountId = By.id("newAccountId");
        wait.until(ExpectedConditions.visibilityOfElementLocated(newAccountId));
        String newAccountNumber = driver.findElement(newAccountId).getText();
        assertFalse(newAccountNumber.isBlank(),
                "The confirmation should show the new account number");

        driver.findElement(By.linkText("Accounts Overview")).click();
        By overviewAccount = By.xpath(
                "//table[@id='accountTable']//a[normalize-space()='" + newAccountNumber + "']");
        wait.until(ExpectedConditions.visibilityOfElementLocated(overviewAccount));
        driver.findElement(overviewAccount).click();

        wait.until(ExpectedConditions.textToBe(By.id("accountId"), newAccountNumber));
        wait.until(ExpectedConditions.textToBe(By.id("accountType"), "CHECKING"));
        assertEquals(newAccountNumber, driver.findElement(By.id("accountId")).getText(),
                "Account details should show the account opened by this test");
        assertEquals("CHECKING", driver.findElement(By.id("accountType")).getText(),
                "Account details should show the expected account type");
    }

    @Test
    public void customerCanVerifyNewAccountDetails() {
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
        driver.findElement(By.linkText("Open New Account")).click();

        By accountType = By.id("type");
        wait.until(ExpectedConditions.visibilityOfElementLocated(accountType));
        new Select(driver.findElement(accountType)).selectByVisibleText("CHECKING");
        By sourceAccount = By.id("fromAccountId");
        wait.until(driver -> !new Select(driver.findElement(sourceAccount)).getOptions().isEmpty());
        driver.findElement(By.cssSelector("input[value='Open New Account']")).click();

        wait.until(ExpectedConditions.textToBePresentInElementLocated(
                By.id("rightPanel"), "Congratulations, your account is now open."));
        By newAccountId = By.id("newAccountId");
        wait.until(ExpectedConditions.visibilityOfElementLocated(newAccountId));
        String newAccountNumber = driver.findElement(newAccountId).getText();
        assertFalse(newAccountNumber.isBlank(),
                "The confirmation should show the new account number");

        driver.findElement(By.linkText("Accounts Overview")).click();
        By overviewAccount = By.xpath(
                "//table[@id='accountTable']//a[normalize-space()='" + newAccountNumber + "']");
        wait.until(ExpectedConditions.visibilityOfElementLocated(overviewAccount));
        driver.findElement(overviewAccount).click();

        wait.until(ExpectedConditions.textToBe(By.id("accountId"), newAccountNumber));
        wait.until(ExpectedConditions.textToBe(By.id("accountType"), "CHECKING"));
        assertEquals(newAccountNumber, driver.findElement(By.id("accountId")).getText(),
                "Account details should show the account opened by this test");
        assertEquals("CHECKING", driver.findElement(By.id("accountType")).getText(),
                "Account details should show the expected account type");
    }
}
