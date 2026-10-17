package stepdefinitions;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TransferFundsSteps {

    // Opens a second checking account funded with $100 from the first account
    @Given("I have a second account")
    public void iHaveASecondAccount() throws InterruptedException {
        WebDriver driver = TestContext.driver;
        driver.findElement(By.linkText("Open New Account")).click();
        Thread.sleep(3000);

        new Select(driver.findElement(By.id("type"))).selectByVisibleText("CHECKING");
        new Select(driver.findElement(By.id("fromAccountId"))).selectByValue(TestContext.firstAccountId);
        driver.findElement(By.xpath("//input[@value='Open New Account']")).click();
        Thread.sleep(3000);

        WebElement newAccount = driver.findElement(By.id("newAccountId"));
        assertTrue(newAccount.isDisplayed(), "Second account was not opened");
        TestContext.secondAccountId = newAccount.getText();
        TestContext.rememberAccountState();
    }

    // Transfers a valid number amount from the first account to the second account
    @When("I transfer ${bigdecimal} from my first account to my second account")
    public void iTransferFromMyFirstAccountToMySecondAccount(BigDecimal amount) throws InterruptedException {
        submitTransfer(amount.toPlainString());
    }

    // Tries to transfer an amount typed exactly as given, including invalid text
    @When("I try to transfer {string} from my first account to my second account")
    public void iTryToTransferFromMyFirstAccountToMySecondAccount(String amount) throws InterruptedException {
        submitTransfer(amount);
    }

    // Checks that the Transfer Complete confirmation is shown
    @Then("I should see that the transfer is complete")
    public void iShouldSeeThatTheTransferIsComplete() {
        WebElement result = TestContext.driver.findElement(By.id("showResult"));
        assertTrue(result.isDisplayed(), "Transfer confirmation was not shown");
        assertEquals("Transfer Complete!", result.findElement(By.className("title")).getText());
    }

    // Checks that the second account balance went up by the given amount
    @Then("my second account balance should increase by ${bigdecimal}")
    public void mySecondAccountBalanceShouldIncreaseBy(BigDecimal amount) throws InterruptedException {
        Map<String, BigDecimal> balancesAfter = TestContext.readBalances();
        BigDecimal before = TestContext.balancesBefore.get(TestContext.secondAccountId);
        BigDecimal after = balancesAfter.get(TestContext.secondAccountId);
        assertEquals(0, before.add(amount).compareTo(after),
                "Second account balance: expected " + before.add(amount) + " but was " + after);
    }

    // Checks that the transfer confirmation did not appear
    @Then("the transfer should not go through")
    public void theTransferShouldNotGoThrough() {
        WebElement result = TestContext.driver.findElement(By.id("showResult"));
        assertFalse(result.isDisplayed(), "The transfer was completed but should have been refused");
    }

    // Fills in the Transfer Funds form and submits it
    private void submitTransfer(String amount) throws InterruptedException {
        WebDriver driver = TestContext.driver;
        driver.findElement(By.linkText("Transfer Funds")).click();
        Thread.sleep(3000);

        driver.findElement(By.id("amount")).sendKeys(amount);
        new Select(driver.findElement(By.id("fromAccountId"))).selectByValue(TestContext.firstAccountId);
        new Select(driver.findElement(By.id("toAccountId"))).selectByValue(TestContext.secondAccountId);
        driver.findElement(By.xpath("//input[@value='Transfer']")).click();
        Thread.sleep(3000);
    }
}
