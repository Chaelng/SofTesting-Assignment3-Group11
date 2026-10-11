package pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

public class RegistrationPage extends BasePage {
    private final By registerLink = By.linkText("Register");
    // Locators for customer registration fields.
    private final By firstName = By.id("customer.firstName");
    private final By lastName = By.id("customer.lastName");
    private final By address = By.id("customer.address.street");
    private final By city = By.id("customer.address.city");
    private final By state = By.id("customer.address.state");
    private final By zipCode = By.id("customer.address.zipCode");
    private final By phone = By.id("customer.phoneNumber");
    private final By ssn = By.id("customer.ssn");

    private final By username = By.id("customer.username");
    private final By password = By.id("customer.password");
    private final By confirmPassword = By.id("repeatedPassword");

    private final By registerButton =
            By.cssSelector("input[value='Register']");

    public RegistrationPage(WebDriver driver, WebDriverWait wait) {
        super(driver, wait);
    }

    public void openRegistration() {
        click(registerLink);
    }
    /**
     * Completes and submits the registration form
     * using valid customer data.
     */
    public void registerNewUser(String user, String pass) {

        type(firstName, "Group");
        type(lastName, "Eleven");
        type(address, "124 Test Street");
        type(city, "Melbourne");
        type(state, "VIC");
        type(zipCode, "3000");
        type(phone, "0400000000");
        type(ssn, "123456789");

        type(username, user);
        type(password, pass);
        type(confirmPassword, pass);

        click(registerButton);
    }
    public void registerWithMismatchedPasswords(String user) {

        type(firstName, "Group");
        type(lastName, "Eleven");
        type(address, "124 Test Street");
        type(city, "Melbourne");
        type(state, "VIC");
        type(zipCode, "3000");
        type(phone, "0400000000");
        type(ssn, "123456789");

        type(username, user);
        type(password, "Test1234");
        type(confirmPassword, "Different1234");

        click(registerButton);
    }
}