package stepdefinitions;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.chrome.ChromeDriver;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Hooks {

    // Hides Selenium's harmless "CDP version" warning so the console stays readable
    private static final Logger CDP_LOGGER = Logger.getLogger("org.openqa.selenium.devtools");

    static {
        CDP_LOGGER.setLevel(Level.SEVERE);
    }

    // Opens a new Chrome window before every scenario
    @Before
    public void openBrowser() {
        TestContext.reset();
        TestContext.driver = new ChromeDriver();
        TestContext.driver.manage().window().maximize();
    }

    // Attaches a screenshot and pauses 3 seconds if the scenario failed, then closes Chrome
    @After
    public void closeBrowser(Scenario scenario) throws InterruptedException {
        if (TestContext.driver == null) {
            return;
        }
        if (scenario.isFailed()) {
            byte[] screenshot = ((TakesScreenshot) TestContext.driver).getScreenshotAs(OutputType.BYTES);
            scenario.attach(screenshot, "image/png", scenario.getName());
            Thread.sleep(3000);
        }
        TestContext.driver.quit();
        TestContext.driver = null;
    }
}
