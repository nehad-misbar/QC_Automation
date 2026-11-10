package com.erp.automation.core;

import org.openqa.selenium.WebDriver;

public final class DriverManager {

    private DriverManager() {
        // Prevent object creation
    }

    private static final ThreadLocal<WebDriver> DRIVER =
            new ThreadLocal<>();

    public static void setDriver(WebDriver driver) {
        DRIVER.set(driver);
    }

    public static WebDriver getDriver() {

        WebDriver driver = DRIVER.get();

        if (driver == null) {
            throw new IllegalStateException(
                    "WebDriver has not been initialized."
            );
        }

        return driver;
    }

    public static void quitDriver() {

        WebDriver driver = DRIVER.get();

        if (driver != null) {
            driver.quit();
            DRIVER.remove();
        }
    }
}