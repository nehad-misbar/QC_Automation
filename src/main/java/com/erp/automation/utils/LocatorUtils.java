package com.erp.automation.utils;

import org.openqa.selenium.By;

public final class LocatorUtils {

    private LocatorUtils() {
        // Prevent object creation
    }

    // =========================
    // Test ID
    // =========================

    public static By testId(String value) {

        return By.cssSelector(
                "[data-testid='" + value + "']"
        );
    }

    // =========================
    // ID
    // =========================

    public static By id(String value) {

        return By.id(value);
    }

    // =========================
    // Name
    // =========================

    public static By name(String value) {

        return By.name(value);
    }

    // =========================
    // Exact Text
    // =========================

    public static By text(String value) {

        return By.xpath(
                "//*[normalize-space()='" + value + "']"
        );
    }

    // =========================
    // Contains Text
    // =========================

    public static By textContains(String value) {

        return By.xpath(
                "//*[contains(normalize-space(),'" + value + "')]"
        );
    }

    // =========================
    // CSS
    // =========================

    public static By css(String value) {

        return By.cssSelector(value);
    }

    // =========================
    // XPath
    // =========================

    public static By xpath(String value) {

        return By.xpath(value);
    }

    // =========================
    // Class Name
    // =========================

    public static By className(String value) {

        return By.className(value);
    }

    // =========================
    // Tag Name
    // =========================

    public static By tagName(String value) {

        return By.tagName(value);
    }
}