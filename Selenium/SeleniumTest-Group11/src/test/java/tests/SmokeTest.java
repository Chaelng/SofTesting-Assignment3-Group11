package tests;

import base.BaseTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class SmokeTest extends BaseTest {

    @Test
    public void parabankHomePageLoadsSuccessfully() {
        driver.get("https://parabank.parasoft.com/parabank/index.htm");

        assertTrue(
                driver.getTitle().toLowerCase().contains("parabank"),
                "ParaBank home page should load successfully"
        );
    }
}