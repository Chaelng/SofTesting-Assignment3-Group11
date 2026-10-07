import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

public class Main {
    public static void main(String[] args) {

        System.setProperty("webdriver.chrome.driver","chromedriver.exe");

        WebDriver driver = new ChromeDriver();
        driver.get("https://www.google.com.au");
        //WebElement element = driver.findElement(By.name("q"));
        //WebElement element = driver.findElement(By.xpath("//*[@id=\'APjFqb\']"));
        //WebElement element = driver.findElement(By.cssSelector("input.gLFyf"));
        //element.sendKeys("Cheese");
        //element.submit();

        //WebElement element = driver.findElement(By.linkText("Gmail"));
        WebElement element = driver.findElement(By.linkText("Sign in"));

        element.click();
    }
}