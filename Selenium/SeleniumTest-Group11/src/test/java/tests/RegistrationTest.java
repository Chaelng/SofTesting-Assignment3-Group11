package tests;

import base.BaseTest;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import pages.RegistrationPage;
import utils.TestData;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class RegistrationTest extends BaseTest {

    @Test
    public void userCanRegisterSuccessfully() {

        String username = TestData.uniqueUsername();

        RegistrationPage registrationPage =
                new RegistrationPage(driver, wait);

        registrationPage.openRegistration();

        registrationPage.registerNewUser(
                username,
                TestData.PASSWORD
        );

        // Wait specifically for the registration success message
        wait.until(
                ExpectedConditions.textToBePresentInElementLocated(
                        By.id("rightPanel"),
                        "Your account was created successfully"
                )
        );

        String pageText =
                driver.findElement(By.id("rightPanel")).getText();

        System.out.println("Registration result:");
        System.out.println(pageText);

        assertTrue(
                pageText.contains("Your account was created successfully"),
                "Registration should create the account successfully"
        );

        assertTrue(
                pageText.contains(username),
                "Successful registration should display the username"
        );
    }
    @Test
    public void registrationRejectsMismatchedPasswords() {

        String username = TestData.uniqueUsername();

        RegistrationPage registrationPage =
                new RegistrationPage(driver, wait);

        registrationPage.openRegistration();

        registrationPage.registerWithMismatchedPasswords(username);

        String pageText =
                driver.findElement(By.id("rightPanel")).getText();

        assertTrue(
                pageText.contains("Passwords did not match"),
                "Registration should reject mismatched passwords"
        );
    }
}