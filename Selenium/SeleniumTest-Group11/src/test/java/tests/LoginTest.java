package tests;

import base.BaseTest;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import pages.LoginPage;
import pages.RegistrationPage;
import utils.TestData;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Selenium automated tests for ParaBank customer login.
 *
 * This class verifies successful login as well as
 * invalid and blank credential scenarios.
 */
public class LoginTest extends BaseTest {

    /**
     * Verifies that a registered ParaBank customer can
     * successfully log in using valid credentials.
     *
     * Spira Test Case: TC:53795
     */
    @Test
    public void registeredUserCanLoginSuccessfully() {

        // Generate a unique customer so this test does not
        // depend on previously stored account data.
        String username = TestData.uniqueUsername();

        RegistrationPage registrationPage =
                new RegistrationPage(driver, wait);

        // Register a new customer before testing login.
        registrationPage.openRegistration();

        registrationPage.registerNewUser(
                username,
                TestData.PASSWORD
        );

        // Wait until ParaBank confirms successful registration.
        wait.until(
                ExpectedConditions.textToBePresentInElementLocated(
                        By.id("rightPanel"),
                        "Your account was created successfully"
                )
        );

        LoginPage loginPage =
                new LoginPage(driver, wait);

        // Registration automatically logs the customer in,
        // so log out before testing the login functionality.
        loginPage.logout();

        // Log back in using the newly created credentials.
        loginPage.login(
                username,
                TestData.PASSWORD
        );

        // Wait until the authenticated account page appears.
        wait.until(
                ExpectedConditions.textToBePresentInElementLocated(
                        By.id("rightPanel"),
                        "Accounts Overview"
                )
        );

        // Verify that login was successful.
        assertTrue(
                loginPage.getResultText().contains("Accounts Overview"),
                "Registered user should log in successfully"
        );
    }

    /**
     * Verifies that ParaBank rejects an authentication
     * attempt using invalid credentials.
     *
     * Spira Test Case: TC:53796
     */
    @Test
    public void loginRejectsInvalidCredentials() {

        LoginPage loginPage =
                new LoginPage(driver, wait);

        // Generate a username that should not exist in ParaBank.
        String invalidUsername =
                "invalid_" + System.currentTimeMillis();

        // Attempt to log in using invalid credentials.
        loginPage.login(
                invalidUsername,
                "WrongPassword123"
        );

        // Wait until ParaBank displays the authentication error.
        wait.until(
                ExpectedConditions.textToBePresentInElementLocated(
                        By.id("rightPanel"),
                        "could not be verified"
                )
        );

        // Verify that invalid credentials were rejected.
        assertTrue(
                loginPage.getResultText().contains("could not be verified"),
                "Invalid credentials should be rejected"
        );
    }

    /**
     * Verifies that ParaBank rejects a login attempt
     * when both username and password are left blank.
     *
     * Spira Test Case: TC:53797
     */
    @Test
    public void loginRejectsBlankCredentials() {

        LoginPage loginPage =
                new LoginPage(driver, wait);

        // Submit the login form without entering credentials.
        loginPage.login("", "");

        // Wait until ParaBank displays the validation message.
        wait.until(
                ExpectedConditions.textToBePresentInElementLocated(
                        By.id("rightPanel"),
                        "Please enter a username and password"
                )
        );

        // Capture the result displayed on the page.
        String pageText =
                loginPage.getResultText();

        // Verify that blank credentials were rejected.
        assertTrue(
                pageText.contains("Please enter a username and password"),
                "Blank credentials should be rejected"
        );
    }
}