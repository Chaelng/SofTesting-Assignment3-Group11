package stepdefinitions;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

import static org.junit.jupiter.api.Assertions.assertTrue;

// Temporary: delete together with setup_check.feature.
public class SetupCheckSteps {
    private WebDriver driver;

    @Given("I open the ParaBank home page")
    public void iOpenTheParaBankHomePage() {
        driver = new ChromeDriver();
        driver.get("https://parabank.parasoft.com/parabank/index.htm");
    }

    @Then("the page title should contain {string}")
    public void thePageTitleShouldContain(String expected) {
        try {
            assertTrue(driver.getTitle().contains(expected));
        } finally {
            driver.quit();
        }
    }
}
