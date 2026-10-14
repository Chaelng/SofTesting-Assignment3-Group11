package tests;

import base.BaseTest;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import pages.RegistrationPage;
import utils.TestData;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class RegistrationTest extends BaseTest {

    /**
     * Verifies that a customer can register successfully
     * using valid and unique registration details.
     */
    @Test
    public void userCanRegisterSuccessfully() {

        // Generate a unique username to avoid clashes with existing ParaBank users.
        String username = TestData.uniqueUsername();

        // Open the registration page using the Page Object.
        RegistrationPage registrationPage =
                new RegistrationPage(driver, wait);

        registrationPage.openRegistration();

        // Submit valid customer registration details.
        registrationPage.registerNewUser(
                username,
                TestData.PASSWORD
        );

        // Wait until ParaBank confirms that the account was created.
        wait.until(
                ExpectedConditions.textToBePresentInElementLocated(
                        By.id("rightPanel"),
                        "Your account was created successfully"
                )
        );

        String pageText =
                driver.findElement(By.id("rightPanel")).getText();

        // Verify that registration completed successfully.
        assertTrue(
                pageText.contains("Your account was created successfully"),
                "Registration should create the customer account successfully"
        );

        // Verify that the newly generated username appears on the success page.
        assertTrue(
                pageText.contains(username),
                "Successful registration should display the new username"
        );
    }

    /**
     * Verifies that registration is rejected when the password
     * and confirmation password do not match.
     */
    @Test
    public void registrationRejectsMismatchedPasswords() {

        // Use a fresh username so any failure is caused only by password mismatch.
        String username = TestData.uniqueUsername();

        RegistrationPage registrationPage =
                new RegistrationPage(driver, wait);

        registrationPage.openRegistration();

        // Submit registration with two different password values.
        registrationPage.registerWithMismatchedPasswords(username);

        // Wait for the expected validation message.
        wait.until(
                ExpectedConditions.textToBePresentInElementLocated(
                        By.id("rightPanel"),
                        "Passwords did not match"
                )
        );

        String pageText =
                driver.findElement(By.id("rightPanel")).getText();

        // Confirm that ParaBank rejects the invalid registration.
        assertTrue(
                pageText.contains("Passwords did not match"),
                "Registration should reject mismatched passwords"
        );
    }
}