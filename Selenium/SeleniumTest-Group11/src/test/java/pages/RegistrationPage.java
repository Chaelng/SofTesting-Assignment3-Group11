package pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Page Object representing the ParaBank customer registration page.
 * Contains locators and reusable actions required for registration tests.
 */
public class RegistrationPage extends BasePage {

    // Link used to open the customer registration form.
    private final By registerLink = By.linkText("Register");

    // Customer information fields.
    private final By firstName = By.id("customer.firstName");
    private final By lastName = By.id("customer.lastName");
    private final By address = By.id("customer.address.street");
    private final By city = By.id("customer.address.city");
    private final By state = By.id("customer.address.state");
    private final By zipCode = By.id("customer.address.zipCode");
    private final By phone = By.id("customer.phoneNumber");
    private final By ssn = By.id("customer.ssn");

    // Account credential fields.
    private final By username = By.id("customer.username");
    private final By password = By.id("customer.password");
    private final By confirmPassword = By.id("repeatedPassword");

    // Registration submission button.
    private final By registerButton =
            By.cssSelector("input[value='Register']");

    /**
     * Creates the RegistrationPage using the shared WebDriver and wait.
     */
    public RegistrationPage(WebDriver driver, WebDriverWait wait) {
        super(driver, wait);
    }

    /**
     * Opens the ParaBank registration page.
     */
    public void openRegistration() {
        click(registerLink);
    }

    /**
     * Completes the registration form using valid customer details.
     *
     * @param user unique username for the new customer
     * @param pass password used for the new account
     */
    public void registerNewUser(String user, String pass) {

        // Enter valid customer information.
        type(firstName, "Group");
        type(lastName, "Eleven");
        type(address, "124 Test Street");
        type(city, "Melbourne");
        type(state, "VIC");
        type(zipCode, "3000");
        type(phone, "0400000000");
        type(ssn, "123456789");

        // Enter valid account credentials.
        type(username, user);
        type(password, pass);
        type(confirmPassword, pass);

        // Submit the registration form.
        click(registerButton);
    }

    /**
     * Attempts registration using different password and
     * confirmation password values.
     *
     * @param user unique username for the attempted registration
     */
    public void registerWithMismatchedPasswords(String user) {

        // Enter otherwise valid customer information.
        type(firstName, "Group");
        type(lastName, "Eleven");
        type(address, "124 Test Street");
        type(city, "Melbourne");
        type(state, "VIC");
        type(zipCode, "3000");
        type(phone, "0400000000");
        type(ssn, "123456789");

        // Intentionally enter mismatching passwords.
        type(username, user);
        type(password, "Test1234");
        type(confirmPassword, "Different1234");

        // Submit the invalid registration request.
        click(registerButton);
    }
}