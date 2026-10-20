package tests;

import base.BaseTest;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import pages.RegistrationPage;
import utils.TestData;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Selenium tests for ParaBank customer registration.
 *
 * This class verifies successful registration and
 * validation of mismatched passwords.
 */
public class RegistrationTest extends BaseTest {

    /**
     * Verifies that a new customer can register successfully
     * using valid details and a unique username.
     *
     * Spira Test Case: TC:53791
     */
    @Test
    public void userCanRegisterSuccessfully() {

        // Generate a unique username so repeated test executions
        // do not conflict with an existing ParaBank account.
        String username = TestData.uniqueUsername();

        RegistrationPage registrationPage =
                new RegistrationPage(driver, wait);

        // Open the ParaBank customer registration form.
        registrationPage.openRegistration();

        // Register a new customer using valid information.
        registrationPage.registerNewUser(
                username,
                TestData.PASSWORD
        );

        // Wait until ParaBank confirms successful account creation.
        wait.until(
                ExpectedConditions.textToBePresentInElementLocated(
                        By.id("rightPanel"),
                        "Your account was created successfully"
                )
        );

        // Capture the registration result displayed by ParaBank.
        String pageText =
                driver.findElement(By.id("rightPanel")).getText();

        // Verify that the account was successfully created.
        assertTrue(
                pageText.contains("Your account was created successfully"),
                "Registration should create the customer account successfully"
        );

        // Verify that the generated username appears in the result.
        assertTrue(
                pageText.contains(username),
                "Successful registration should display the new username"
        );
    }

    /**
     * Verifies that registration is rejected when
     * Password and Confirm Password contain different values.
     *
     * Spira Test Case: TC:53794
     */
    @Test
    public void registrationRejectsMismatchedPasswords() {

        // Generate a fresh username so the password mismatch
        // is the only invalid condition in this test.
        String username = TestData.uniqueUsername();

        RegistrationPage registrationPage =
                new RegistrationPage(driver, wait);

        // Open the ParaBank customer registration form.
        registrationPage.openRegistration();

        // Attempt registration using mismatched password values.
        registrationPage.registerWithMismatchedPasswords(username);

        // Wait until ParaBank displays the expected validation message.
        wait.until(
                ExpectedConditions.textToBePresentInElementLocated(
                        By.id("rightPanel"),
                        "Passwords did not match"
                )
        );

        // Capture the validation result.
        String pageText =
                driver.findElement(By.id("rightPanel")).getText();

        // Verify that ParaBank rejects mismatched passwords.
        assertTrue(
                pageText.contains("Passwords did not match"),
                "Registration should reject mismatched passwords"
        );
    }
}