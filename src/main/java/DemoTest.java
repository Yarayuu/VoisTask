import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.reporter.ExtentHtmlReporter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.time.Duration;

public class DemoTest {

    private ExtentReports extentReports;
    private ExtentTest extentTest;
    private WebDriver driver;

    @BeforeEach
    public void setUp() {
        extentReports = new ExtentReports();
        ExtentHtmlReporter htmlReporter = new ExtentHtmlReporter("extent-report.html");
        extentReports.attachReporter(htmlReporter);
    }

    @AfterEach
    public void tearDown() {
        extentReports.flush();
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    public void testSearchExecution() {
        extentTest = extentReports.createTest("Search Execution Test");

        Properties config = loadConfig();
        if (config == null) {
            extentTest.log(Status.FAIL, "Failed to load config properties");
            return;
        }

        initializeWebDriver(config);

        try {
            String urlSearch = config.getProperty("UrlSearch");
            driver.get(urlSearch);

            String testDataPath = config.getProperty("testDataPath");
            Sheet testDataSheet = readTestDataFromExcel(testDataPath);

            executeSearch(testDataSheet);

            extentTest.log(Status.PASS, "Test passed: Successfully executed the search");

        } catch (Exception e) {
            extentTest.log(Status.FAIL, "Test failed: " + e.getMessage());
        }
    }

    private Properties loadConfig() {
        Properties prop = new Properties();
        try (InputStream input = DemoTest.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                extentTest.log(Status.FAIL, "Sorry, unable to find config.properties");
                return null;
            }
            prop.load(input);
        } catch (IOException e) {
            extentTest.log(Status.FAIL, "Error loading config properties: " + e.getMessage());
            return null;
        }
        return prop;
    }

    private void initializeWebDriver(Properties config) {
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
                driver = new FirefoxDriver();
                driver.manage().timeouts().setScriptTimeout(30, TimeUnit.SECONDS);// increase the  timeout
                break;
            default:
                extentTest.log(Status.FAIL, "Invalid browser specified in the config file");
        }

        if (driver != null) {
            driver.manage().window().maximize();
        }
    }

    private Sheet readTestDataFromExcel(String filePath) {
        try (InputStream input = new FileInputStream(filePath)) {
            Workbook workbook = WorkbookFactory.create(input);
            return workbook.getSheetAt(0); // the data is in the first sheet
        } catch (Exception e) {
            extentTest.log(Status.FAIL, "Error reading Excel file: " + e.getMessage());
            return null;
        }
    }

    private void executeSearch(Sheet testDataSheet) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(60));

        try {
            for (int row = 1; row <= testDataSheet.getLastRowNum(); row++) {
                Row dataRow = testDataSheet.getRow(row);

                String searchQuery = dataRow.getCell(0).getStringCellValue();
                int totalPages = (int) dataRow.getCell(1).getNumericCellValue();
                int scrollBy = (int) dataRow.getCell(2).getNumericCellValue();

                WebElement searchBox = driver.findElement(By.id("sb_form_q"));
                searchBox.clear();
                searchBox.sendKeys(searchQuery);

                driver.findElement(By.id("sb_form_go")).submit(); // click on submit button

                JavascriptExecutor js = (JavascriptExecutor) driver;
                js.executeScript("window.scrollBy(0, " + scrollBy + ");"); // scroll down

                wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("b_tween_searchResults")));

                String page2Results = null;

                for (int currentPage = 1; currentPage <= totalPages; currentPage++) {
                    // Wait for the list elements to be present
                    wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("b_tween_searchResults")));

                    // Retrieve and print the results on the current page
                    WebElement ec = driver.findElement(By.id("b_tween_searchResults"));
                    String currentResults = ec.getText();
                    System.out.println("Results for search query '" + searchQuery + "' on page " + currentPage + ":\n" + currentResults);

                    // Compare results on page 2 and page 3
                    if (currentPage == 2) {
                        page2Results = currentResults;
                    } else if (currentPage > 2) {
                        String[] currentPageValues = currentResults.split(" ")[0].split("-");
                        String[] page2Values = page2Results.split(" ")[0].split("-");

                        int currentPageStart = Integer.parseInt(currentPageValues[0]);
                        int currentPageEnd = Integer.parseInt(currentPageValues[1]);
                        int page2Start = Integer.parseInt(page2Values[0]);
                        int page2End = Integer.parseInt(page2Values[1]);

                        if ((currentPageEnd - currentPageStart) == (page2End - page2Start)) {
                            extentTest.log(Status.PASS, "Results for search query '" + searchQuery + "' on page " + currentPage +
                                    " are equal to page " + (currentPage - 1));
                        } else {
                            extentTest.log(Status.FAIL, "Results for search query '" + searchQuery + "' on page " + currentPage +
                                    " are NOT equal to page " + (currentPage - 1));
                        }
                    }

                    if (currentPage < totalPages) {
                        WebElement nextArrow = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//a[@title='Next page']")));
                        js.executeScript("arguments[0].scrollIntoView();", nextArrow);
                        nextArrow.click();
                    }
                }
            }
        } catch (StaleElementReferenceException e) {
            extentTest.log(Status.FAIL, "Stale element reference occurred: " + e.getMessage());
        } catch (Exception e) {
            extentTest.log(Status.FAIL, "Exception occurred: " + e.getMessage());
        }
    }
}
