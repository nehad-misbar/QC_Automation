package com.erp.automation.base;

import com.erp.automation.config.ConfigReader;
import com.erp.automation.core.DriverFactory;
import com.erp.automation.core.DriverManager;
import com.erp.automation.listeners.TestListener;

import org.openqa.selenium.WebDriver;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;

@Listeners(TestListener.class)
public abstract class BaseTest {

    protected WebDriver driver;

    @BeforeMethod(alwaysRun = true)
    public void setUp() {

        driver = DriverFactory.createDriver(
                ConfigReader.get("browser")
        );

        DriverManager.setDriver(driver);

        driver.manage().window().maximize();
    }

    /**
     * Closes the browser after every test.
     *
     * <p>Leaving every Chrome open made a full class run fail: after the
     * third test the machine was overloaded and login/navigation timed out
     * ("Timed out receiving message from renderer"), so 21 of 23 tests in
     * PurchaseOrderTest were skipped (mvn test, 2026-10-28).</p>
     *
     * <p>To keep the browser open while debugging a single test, add
     * {@code keepBrowserOpen=true} to config.properties.</p>
     *
     * <p>The failure screenshot is taken here, BEFORE closing the browser.</p>
     */
    @AfterMethod(alwaysRun = true)
    public void tearDown(ITestResult result) {

        if (result.getStatus() == ITestResult.FAILURE) {
            TestListener.captureFailureScreenshot(result);
        }

        if (!keepBrowserOpen()) {
            DriverManager.quitDriver();
        }

        driver = null;
    }

    /** Optional key; missing or anything but "true" means: close the browser. */
    private static boolean keepBrowserOpen() {
        try {
            return Boolean.parseBoolean(ConfigReader.get("keepBrowserOpen"));
        } catch (RuntimeException missingKey) {
            return false;
        }
    }
}
