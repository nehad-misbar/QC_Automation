package com.erp.automation.pages;

import com.erp.automation.utils.WaitManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public abstract class BasePage {

    protected final WebDriver driver;
    protected final WaitManager wait;

    protected BasePage(WebDriver driver) {

        this.driver = driver;
        this.wait = new WaitManager(driver);
    }

    // =========================
    // Click
    // =========================

    protected void click(WebElement element) {

        wait.waitForClickable(element).click();
    }

    protected void click(By locator) {

        wait.waitForClickable(locator).click();
    }

    // =========================
    // Type
    // =========================

    protected void type(WebElement element, String text) {

        WebElement visibleElement =
                wait.waitForVisible(element);

        visibleElement.clear();
        visibleElement.sendKeys(text);
    }

    protected void type(By locator, String text) {

        WebElement element =
                wait.waitForVisible(locator);

        element.clear();
        element.sendKeys(text);
    }

    // =========================
    // Clear
    // =========================

    protected void clear(WebElement element) {

        wait.waitForVisible(element).clear();
    }

    protected void clear(By locator) {

        wait.waitForVisible(locator).clear();
    }

    // =========================
    // Get Text
    // =========================

    protected String getText(WebElement element) {

        return wait
                .waitForVisible(element)
                .getText();
    }

    protected String getText(By locator) {

        return wait
                .waitForVisible(locator)
                .getText();
    }

    // =========================
    // Get Attribute
    // =========================

    protected String getAttribute(
            WebElement element,
            String attribute) {

        return wait
                .waitForVisible(element)
                .getAttribute(attribute);
    }

    protected String getAttribute(
            By locator,
            String attribute) {

        return wait
                .waitForVisible(locator)
                .getAttribute(attribute);
    }

    // =========================
    // Displayed
    // =========================

    protected boolean isDisplayed(By locator) {

        return wait
                .waitForVisible(locator)
                .isDisplayed();
    }

    protected boolean isDisplayed(WebElement element) {

        return wait
                .waitForVisible(element)
                .isDisplayed();
    }

    // =========================
    // Enabled
    // =========================

    protected boolean isEnabled(By locator) {

        return wait
                .waitForVisible(locator)
                .isEnabled();
    }

    protected boolean isEnabled(WebElement element) {

        return wait
                .waitForVisible(element)
                .isEnabled();
    }

    // =========================
    // Selected
    // =========================

    protected boolean isSelected(By locator) {

        return wait
                .waitForVisible(locator)
                .isSelected();
    }

    protected boolean isSelected(WebElement element) {

        return wait
                .waitForVisible(element)
                .isSelected();
    }

    // =========================
    // Submit
    // =========================

    protected void submit(By locator) {

        wait
                .waitForVisible(locator)
                .submit();
    }

    protected void submit(WebElement element) {

        wait
                .waitForVisible(element)
                .submit();
    }

    // =========================
    // Browser Navigation
    // =========================

    protected void navigateTo(String url) {

        driver.get(url);
    }

    protected void refreshPage() {

        driver.navigate().refresh();
    }

    protected void goBack() {

        driver.navigate().back();
    }

    protected void goForward() {

        driver.navigate().forward();
    }

    // =========================
    // Browser Information
    // =========================

    protected String getCurrentUrl() {

        return driver.getCurrentUrl();
    }

    protected String getTitle() {

        return driver.getTitle();
    }
    // =========================
// Wait Conditions
// =========================

    protected boolean waitForUrlContains(String value) {

        return wait.waitForUrlContains(value);
    }

    protected boolean waitForTitleContains(String value) {

        return wait.waitForTitleContains(value);
    }

    protected boolean waitForText(By locator, String text) {

        return wait.waitForText(locator, text);
    }
}