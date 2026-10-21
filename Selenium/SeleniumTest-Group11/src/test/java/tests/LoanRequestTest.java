package tests;

import base.BaseTest;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import pages.LoginPage;
import pages.RegistrationPage;
import utils.TestData;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class LoanRequestTest extends BaseTest {

    /**
     * Verifies that a customer can submit a loan request with valid details.
     *
     * Spira Test Case: TC:55401
     */
    @Test
    public void customerCanRequestLoanSuccessfully() {
        openLoanRequestForm();
        enterLoanRequest("200", "25");
        driver.findElement(By.cssSelector("input[value='Apply Now']")).click();

        By loanStatus = By.id("loanStatus");
        wait.until(ExpectedConditions.visibilityOfElementLocated(loanStatus));
        assertTrue(driver.findElement(loanStatus).getText().contains("Approved"),
                "ParaBank should approve the eligible loan request");
    }

    /**
     * Verifies that a loan request is rejected when its amount is missing.
     *
     * Spira Test Case: TC:55409
     */
    @Test
    public void loanRequestRejectsMissingAmount() {
        openLoanRequestForm();
        enterLoanRequest("", "25");
        driver.findElement(By.cssSelector("input[value='Apply Now']")).click();

        assertTrue(driver.findElement(By.id("requestLoanForm")).isDisplayed(),
                "The loan request form should remain visible when the amount is blank");
        assertTrue(driver.findElement(By.id("amount")).getAttribute("value").isBlank(),
                "The loan amount should remain blank after the request is rejected");
        assertTrue(driver.findElements(By.id("loanStatus")).stream()
                        .noneMatch(element -> element.isDisplayed()),
                "ParaBank should not display a loan decision for a request without an amount");
    }

    /**
     * Verifies that a request requiring a down payment above the source balance is denied.
     *
     * Spira Test Case: TC:55447
     */
    @Test
    public void loanRequestDeniedWhenDownPaymentExceedsBalance() {
        openLoanRequestForm();
        enterLoanRequest("5000", "25");
        driver.findElement(By.cssSelector("input[value='Apply Now']")).click();

        By loanStatus = By.id("loanStatus");
        wait.until(ExpectedConditions.visibilityOfElementLocated(loanStatus));
        assertTrue(driver.findElement(loanStatus).getText().contains("Denied"),
                "ParaBank should deny a loan request when the down payment is unaffordable");
    }

    private void openLoanRequestForm() {
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

        driver.findElement(By.linkText("Request Loan")).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("requestLoanForm")));
    }

    private void enterLoanRequest(String amount, String downPayment) {
        driver.findElement(By.id("amount")).sendKeys(amount);
        driver.findElement(By.id("downPayment")).sendKeys(downPayment);

        By sourceAccount = By.id("fromAccountId");
        wait.until(driver -> !new Select(driver.findElement(sourceAccount)).getOptions().isEmpty());
        new Select(driver.findElement(sourceAccount)).selectByIndex(0);
    }
}
