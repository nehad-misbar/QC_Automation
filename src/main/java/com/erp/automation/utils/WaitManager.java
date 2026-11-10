package com.erp.automation.utils;

import com.erp.automation.config.ConfigReader;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class WaitManager {

    private final WebDriverWait wait;

    public WaitManager(WebDriver driver) {

        int timeout =
                ConfigReader.getInt("explicitWait");

        this.wait = new WebDriverWait(
                driver,
                Duration.ofSeconds(timeout)
        );
    }

    // =========================
    // Wait for Visible
    // =========================

    public WebElement waitForVisible(WebElement element) {

        return wait.until(
                ExpectedConditions.visibilityOf(element)
        );
    }

    public WebElement waitForVisible(By locator) {

        return wait.until(
                ExpectedConditions.visibilityOfElementLocated(locator)
        );
    }

    // =========================
    // Wait for Clickable
    // =========================

    public WebElement waitForClickable(WebElement element) {

        return wait.until(
                ExpectedConditions.elementToBeClickable(element)
        );
    }

    public WebElement waitForClickable(By locator) {

        return wait.until(
                ExpectedConditions.elementToBeClickable(locator)
        );
    }

    // =========================
    // Wait for Presence
    // =========================

    public WebElement waitForPresence(By locator) {

        return wait.until(
                ExpectedConditions.presenceOfElementLocated(locator)
        );
    }

    // =========================
    // Wait for URL
    // =========================

    public boolean waitForUrlContains(String value) {

        return wait.until(
                ExpectedConditions.urlContains(value)
        );
    }

    // =========================
    // Wait for Title
    // =========================

    public boolean waitForTitleContains(String value) {

        return wait.until(
                ExpectedConditions.titleContains(value)
        );
    }

    // =========================
    // Wait for Text
    // =========================

    public boolean waitForText(
            By locator,
            String text) {

        return wait.until(
                ExpectedConditions.textToBePresentInElementLocated(
                        locator,
                        text
                )
        );
    }

    // =========================
    // Wait for Invisible
    // =========================

    public boolean waitForInvisible(By locator) {

        return wait.until(
                ExpectedConditions.invisibilityOfElementLocated(
                        locator
                )
        );
    }

    // =========================
    // Wait for Selected
    // =========================

    public boolean waitForSelected(By locator) {

        return wait.until(
                ExpectedConditions.elementToBeSelected(locator)
        );
    }
}