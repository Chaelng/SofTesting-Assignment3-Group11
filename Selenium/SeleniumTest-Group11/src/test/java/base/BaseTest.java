package base;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class BaseTest {

    // Shared WebDriver instance used by all Selenium test classes.
    protected WebDriver driver;

    // Explicit wait used to synchronise Selenium with dynamic page content.
    protected WebDriverWait wait;

    protected static final String BASE_URL =
            "https://parabank.parasoft.com/parabank";
    /**
     * Starts a fresh Chrome browser before every test
     * and opens the ParaBank home page.
     */
    @BeforeEach
    public void setUp() {
        driver = new ChromeDriver();
        driver.manage().window().maximize();

        wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        driver.get(BASE_URL + "/index.htm");
    }
    /**
     * Closes the browser after every test to ensure
     * each test runs independently.
     */
    @AfterEach
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
}