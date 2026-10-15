package stepdefinitions;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static stepdefinitions.TestContext.data;

public class BillPaySteps {

    // Pays a bill with valid payee details
    @When("I pay a bill of ${bigdecimal} to a valid payee from my first account")
    public void iPayABillToAValidPayeeFromMyFirstAccount(BigDecimal amount) throws InterruptedException {
        submitBillPayment(validPayeeDetails(amount.toPlainString()));
    }

    // Tries to pay a bill with the amount typed exactly as given, including invalid text
    @When("I try to pay a bill of {string} to a valid payee from my first account")
    public void iTryToPayABillToAValidPayeeFromMyFirstAccount(String amount) throws InterruptedException {
        submitBillPayment(validPayeeDetails(amount));
    }

    // Pays a bill with one payee field left empty
    @When("I pay a bill of ${bigdecimal} to a valid payee but leave the {} empty")
    public void iPayABillToAValidPayeeButLeaveTheFieldEmpty(BigDecimal amount, String field) throws InterruptedException {
        Map<String, String> details = validPayeeDetails(amount.toPlainString());
        String inputName = fieldToInputName(field);
        assertTrue(details.containsKey(inputName), "Unknown payee field: " + field);
        details.put(inputName, "");
        submitBillPayment(details);
    }

    // Pays a bill where the verify account number differs from the account number
    @When("I pay a bill of ${bigdecimal} to a payee but enter a different account number to verify")
    public void iPayABillToAPayeeButEnterADifferentAccountNumberToVerify(BigDecimal amount) throws InterruptedException {
        Map<String, String> details = validPayeeDetails(amount.toPlainString());
        details.put("verifyAccount", data("payee.accountNumber") + "2");
        submitBillPayment(details);
    }

    // Pays a bill to a payee with the given account number in both account fields
    @When("I pay a bill of ${bigdecimal} to a payee with the account number {string}")
    public void iPayABillToAPayeeWithTheAccountNumber(BigDecimal amount, String accountNumber) throws InterruptedException {
        Map<String, String> details = validPayeeDetails(amount.toPlainString());
        details.put("payee.accountNumber", accountNumber);
        details.put("verifyAccount", accountNumber);
        submitBillPayment(details);
    }

    // Checks that the Bill Payment Complete confirmation is shown
    @Then("I should see that the bill payment was successful")
    public void iShouldSeeThatTheBillPaymentWasSuccessful() {
        WebElement result = TestContext.driver.findElement(By.id("billpayResult"));
        assertTrue(result.isDisplayed(), "Bill payment confirmation was not shown");
        assertEquals("Bill Payment Complete",
                TestContext.driver.findElement(By.xpath("//div[@id='billpayResult']/h1")).getText());
    }

    // Checks that the bill payment confirmation did not appear
    @Then("the bill payment should not go through")
    public void theBillPaymentShouldNotGoThrough() {
        WebElement result = TestContext.driver.findElement(By.id("billpayResult"));
        assertFalse(result.isDisplayed(), "The bill payment was completed but should have been refused");
    }

    // Builds the valid payee details keyed by each input's name on the form
    private Map<String, String> validPayeeDetails(String amount) {
        Map<String, String> details = new LinkedHashMap<>();
        details.put("payee.name", data("payee.name"));
        details.put("payee.address.street", data("payee.address"));
        details.put("payee.address.city", data("payee.city"));
        details.put("payee.address.state", data("payee.state"));
        details.put("payee.address.zipCode", data("payee.zipCode"));
        details.put("payee.phoneNumber", data("payee.phone"));
        details.put("payee.accountNumber", data("payee.accountNumber"));
        details.put("verifyAccount", data("payee.accountNumber"));
        details.put("amount", amount);
        return details;
    }

    // Maps the field name used in the feature file to the input name on the form
    private String fieldToInputName(String field) {
        switch (field) {
            case "payee name": return "payee.name";
            case "address": return "payee.address.street";
            case "city": return "payee.address.city";
            case "state": return "payee.address.state";
            case "zip code": return "payee.address.zipCode";
            case "phone number": return "payee.phoneNumber";
            case "account number": return "payee.accountNumber";
            case "verify account number": return "verifyAccount";
            default: return field;
        }
    }

    // Fills in the Bill Pay form and sends the payment
    private void submitBillPayment(Map<String, String> details) throws InterruptedException {
        WebDriver driver = TestContext.driver;
        driver.findElement(By.linkText("Bill Pay")).click();
        Thread.sleep(3000);

        for (Map.Entry<String, String> field : details.entrySet()) {
            driver.findElement(By.name(field.getKey())).sendKeys(field.getValue());
        }
        new Select(driver.findElement(By.name("fromAccountId"))).selectByValue(TestContext.firstAccountId);
        driver.findElement(By.cssSelector("input[value='Send Payment']")).click();
        Thread.sleep(3000);
    }
}
