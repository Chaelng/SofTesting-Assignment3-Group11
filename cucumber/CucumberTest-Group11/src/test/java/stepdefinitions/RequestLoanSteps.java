package stepdefinitions;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RequestLoanSteps {

    // Requests a loan with valid number amounts
    @When("I request a loan of ${bigdecimal} with a down payment of ${bigdecimal} from my first account")
    public void iRequestALoanWithADownPaymentFromMyFirstAccount(BigDecimal loanAmount, BigDecimal downPayment) throws InterruptedException {
        submitLoanRequest(loanAmount.toPlainString(), downPayment.toPlainString());
    }

    // Tries to request a loan with the amounts typed exactly as given, including invalid text
    @When("I try to request a loan of {string} with a down payment of {string} from my first account")
    public void iTryToRequestALoanWithADownPaymentFromMyFirstAccount(String loanAmount, String downPayment) throws InterruptedException {
        submitLoanRequest(loanAmount, downPayment);
    }

    // Checks the loan status shown in the result table
    @Then("the loan status should be {string}")
    public void theLoanStatusShouldBe(String status) {
        assertEquals(status, TestContext.driver.findElement(By.id("loanStatus")).getText());
    }

    // Checks that a new loan account number is shown
    @Then("I should be given a new loan account number")
    public void iShouldBeGivenANewLoanAccountNumber() {
        WebElement newAccount = TestContext.driver.findElement(By.id("newAccountId"));
        assertTrue(newAccount.isDisplayed(), "No new loan account number was shown");
        assertFalse(newAccount.getText().isEmpty(), "The new loan account number is empty");
    }

    // Checks that no new loan account number is shown
    @Then("I should not be given a new loan account number")
    public void iShouldNotBeGivenANewLoanAccountNumber() {
        WebElement approved = TestContext.driver.findElement(By.id("loanRequestApproved"));
        assertFalse(approved.isDisplayed(), "A new loan account was created but should not have been");
    }

    // Checks that the loan was not approved, whether it was denied or rejected with an error
    @Then("the loan should not be approved")
    public void theLoanShouldNotBeApproved() {
        WebElement status = TestContext.driver.findElement(By.id("loanStatus"));
        if (status.isDisplayed()) {
            assertNotEquals("Approved", status.getText(), "The loan was approved but should have been refused");
        }
        assertFalse(TestContext.driver.findElement(By.id("loanRequestApproved")).isDisplayed(),
                "The loan was approved but should have been refused");
    }

    // Fills in the Request Loan form and applies
    private void submitLoanRequest(String loanAmount, String downPayment) throws InterruptedException {
        WebDriver driver = TestContext.driver;
        driver.findElement(By.linkText("Request Loan")).click();
        Thread.sleep(3000);

        driver.findElement(By.id("amount")).sendKeys(loanAmount);
        driver.findElement(By.id("downPayment")).sendKeys(downPayment);
        new Select(driver.findElement(By.id("fromAccountId"))).selectByValue(TestContext.firstAccountId);
        driver.findElement(By.xpath("//input[@value='Apply Now']")).click();
        Thread.sleep(3000);
    }
}
