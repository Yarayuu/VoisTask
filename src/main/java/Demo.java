import java.io.InputStream;
import java.time.Duration;
import java.util.Properties;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class Demo {
    public static void main(String[] args) {
        // Load configuration from the config file
        Properties config = loadConfig();

        if (config == null) {
            System.out.println("Failed to load configuration. Exiting.");
            return;
        }

        // Initialize WebDriver based on the specified browser in the config file
        WebDriver driver = initializeWebDriver(config);

        try {
            // Maximize the window and navigate to the specified URL
            driver.manage().window().maximize();
            driver.get(config.getProperty("UrlSearch"));

            // Enter search query
            driver.findElement(By.id("sb_form_q")).sendKeys(config.getProperty("searchQuery"));

            // Submit the search form
            driver.findElement(By.id("sb_form_go")).submit();

            // Scroll down by the specified amount
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript("window.scrollBy(0, " + config.getProperty("scrollBy") + ");");

            // Wait for a specified duration
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));

            // Set the desired number of pages
            int totalPages = Integer.parseInt(config.getProperty("totalPages"));

            String page2Results = null;

            // Loop through the pages
            for (int currentPage = 1; currentPage <= totalPages; currentPage++) {
                // Wait for the list elements to be present
                wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("b_tween_searchResults")));

                // Retrieve and print the results on the current page
                WebElement ec = driver.findElement(By.id("b_tween_searchResults"));
                String currentResults = ec.getText();
                System.out.println("Results on page " + currentPage + ":\n" + currentResults);

                // Compare results on page 2 and page 3
                if (currentPage == 2) {
                    page2Results = currentResults;
                } else if (currentPage == 3) {
                    String[] currentPageValues = currentResults.split(" ")[0].split("-");
                    String[] page2Values = page2Results.split(" ")[0].split("-");

                    int currentPageStart = Integer.parseInt(currentPageValues[0]);
                    int currentPageEnd = Integer.parseInt(currentPageValues[1]);
                    int page2Start = Integer.parseInt(page2Values[0]);
                    int page2End = Integer.parseInt(page2Values[1]);

                    if ((currentPageEnd - currentPageStart) == (page2End - page2Start)) {
                        System.out.println("Results on page " + currentPage + " are equal to page " + (currentPage - 1));
                    } else {
                        System.out.println("Results on page " + currentPage + " are NOT equal to page " + (currentPage - 1));
                    }
                }

                // Your logic for processing the current page goes here

                // Click on the "Next" arrow if not on the last page
                if (currentPage < totalPages) {
                    WebElement nextArrow = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//a[@title='Next page']")));

                    // Scroll to bring the "Next" arrow into view
                    js.executeScript("arguments[0].scrollIntoView();", nextArrow);

                    nextArrow.click();
                }
            }
        } catch (StaleElementReferenceException e) {
            System.out.println("Stale element reference occurred: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Exception occurred: " + e.getMessage());
        } finally {
            // Quit the WebDriver
            if (driver != null) {
                driver.quit();
            }
        }
    }

    private static Properties loadConfig() {
        Properties prop = new Properties();
        try (InputStream input = Demo.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                System.out.println("Sorry, unable to find config.properties");
                return null;
            }
            prop.load(input);
        } catch (Exception e) {
            System.out.println("Error loading config properties: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
        return prop;
    }

    private static WebDriver initializeWebDriver(Properties config) {
        WebDriver driver = null;
        String browser = config.getProperty("browser");

        switch (browser.toLowerCase()) {
        case "chrome":
            System.setProperty("webdriver.chrome.driver", "E:/selenium-Java/Chrome/chromedriver-win64/chromedriver.exe");
            driver = new ChromeDriver();
            break;
        case "edge":
            System.setProperty("webdriver.edge.driver", "E:/selenium-Java/Edge/edgedriver_win64/msedgedriver.exe");
            driver = new EdgeDriver();
            break;
        case "firefox":
            System.setProperty("webdriver.gecko.driver", "E:/selenium-Java/FireFox/geckodriver.exe");
                break;
            default:
                System.out.println("Invalid browser specified in the config file");
                break;
        }

        return driver;
    }
}
