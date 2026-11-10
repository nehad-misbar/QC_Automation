package com.erp.automation.core;

import com.erp.automation.config.ConfigReader;
import io.github.bonigarcia.wdm.WebDriverManager;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

import java.time.Duration;

public final class DriverFactory {

    private DriverFactory() {
        // Prevent object creation
    }

    public static WebDriver createDriver(String browser) {

        if (browser == null || browser.isBlank()) {
            throw new IllegalArgumentException(
                    "Browser cannot be null or empty"
            );
        }

        WebDriver driver = switch (browser.toLowerCase()) {

            case "chrome" -> createChromeDriver();

            case "edge" -> createEdgeDriver();

            case "firefox" -> createFirefoxDriver();

            default -> throw new IllegalArgumentException(
                    "Unsupported browser: " + browser
            );
        };

        configureDriver(driver);

        return driver;
    }

    // =========================
    // Chrome
    // =========================

    private static WebDriver createChromeDriver() {

        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();

        if (ConfigReader.getBoolean("headless")) {
            options.addArguments("--headless=new");
        }

        return new ChromeDriver(options);
    }

    // =========================
    // Edge
    // =========================

    private static WebDriver createEdgeDriver() {

        WebDriverManager.edgedriver().setup();

        EdgeOptions options = new EdgeOptions();

        if (ConfigReader.getBoolean("headless")) {
            options.addArguments("--headless=new");
        }

        return new EdgeDriver(options);
    }

    // =========================
    // Firefox
    // =========================

    private static WebDriver createFirefoxDriver() {

        WebDriverManager.firefoxdriver().setup();

        FirefoxOptions options = new FirefoxOptions();

        if (ConfigReader.getBoolean("headless")) {
            options.addArguments("-headless");
        }

        return new FirefoxDriver(options);
    }

    // =========================
    // Driver Configuration
    // =========================

    private static void configureDriver(WebDriver driver) {

        int pageLoadTimeout =
                ConfigReader.getInt("pageLoadTimeout");

        int implicitWait =
                ConfigReader.getInt("implicitWait");

        driver.manage()
                .timeouts()
                .pageLoadTimeout(
                        Duration.ofSeconds(pageLoadTimeout)
                );

        driver.manage()
                .timeouts()
                .implicitlyWait(
                        Duration.ofSeconds(implicitWait)
                );
    }
}