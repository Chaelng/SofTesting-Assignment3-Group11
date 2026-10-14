package pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

public class LoginPage extends BasePage {

    private final By username =
            By.name("username");

    private final By password =
            By.name("password");

    private final By loginButton =
            By.cssSelector("input[value='Log In']");

    private final By logoutLink =
            By.linkText("Log Out");

    private final By rightPanel =
            By.id("rightPanel");

    public LoginPage(WebDriver driver, WebDriverWait wait) {
        super(driver, wait);
    }

    public void login(String user, String pass) {
        type(username, user);
        type(password, pass);
        click(loginButton);
    }

    public void logout() {
        click(logoutLink);
    }

    public String getResultText() {
        return getText(rightPanel);
    }
}