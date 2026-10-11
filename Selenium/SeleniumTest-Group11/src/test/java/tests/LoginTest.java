package tests;

import base.BaseTest;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import pages.LoginPage;
import pages.RegistrationPage;
import utils.TestData;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class LoginTest extends BaseTest {

    /**
     * Verifies that a previously registered customer
     * can log in using valid credentials.
     */
    @Test
    public void registeredUserCanLoginSuccessfully() {

        // Create a fresh account so the login test does not depend on stored test data.
        String username = TestData.uniqueUsername();

        RegistrationPage registrationPage =
                new RegistrationPage(driver, wait);

        registrationPage.openRegistration();

        registrationPage.registerNewUser(
                username,
                TestData.PASSWORD
        );

        // Wait until account creation has completed.
        wait.until(
                ExpectedConditions.textToBePresentInElementLocated(
                        By.id("rightPanel"),
                        "Your account was created successfully"
                )
        );

        LoginPage loginPage =
                new LoginPage(driver, wait);

        // Registration automatically logs the customer in,
        // so log out before testing the actual login process.
        loginPage.logout();

        // Log back in using the newly created credentials.
        loginPage.login(
                username,
                TestData.PASSWORD
        );

        // Wait until the authenticated account page is displayed.
        wait.until(
                ExpectedConditions.textToBePresentInElementLocated(
                        By.id("rightPanel"),
                        "Accounts Overview"
                )
        );

        // Verify that the customer successfully reached the account page.
        assertTrue(
                loginPage.getResultText().contains("Accounts Overview"),
                "Valid user should be logged in successfully"
        );
    }


    /**
     * Verifies that ParaBank rejects invalid login credentials.
     */
    @Test
    public void loginRejectsInvalidCredentials() {

        LoginPage loginPage =
                new LoginPage(driver, wait);

        // Use credentials that should not exist in the system.
        loginPage.login(
                "invalid_" + System.currentTimeMillis(),
                "WrongPassword123"
        );

        // Wait for ParaBank's authentication error message.
        wait.until(
                ExpectedConditions.textToBePresentInElementLocated(
                        By.id("rightPanel"),
                        "could not be verified"
                )
        );

        // Verify that invalid credentials are not accepted.
        assertTrue(
                loginPage.getResultText().contains("could not be verified"),
                "Invalid credentials should be rejected"
        );
    }
    /**
     * Verifies that ParaBank rejects a login attempt
     * when both username and password are left blank.
     */
    @Test
    public void loginRejectsBlankCredentials() {

        LoginPage loginPage =
                new LoginPage(driver, wait);

        // Submit the login form without entering any credentials.
        loginPage.login("", "");

        // Wait until ParaBank displays an authentication error.
        wait.until(
                ExpectedConditions.textToBePresentInElementLocated(
                        By.id("rightPanel"),
                        "Please enter a username and password"
                )
        );

        String pageText = loginPage.getResultText();

        // Verify that blank credentials are not accepted.
        assertTrue(
                pageText.contains("Please enter a username and password"),
                "Blank credentials should be rejected"
        );
    }
}