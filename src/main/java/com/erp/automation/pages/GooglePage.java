package com.erp.automation.pages;

import com.erp.automation.utils.LocatorUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class GooglePage extends BasePage {

    private final By searchBox =
            LocatorUtils.name("q");

    public GooglePage(WebDriver driver) {
        super(driver);
    }

    public GooglePage open(String url) {

        navigateTo(url);

        return this;
    }

    public GooglePage search(String text) {

        type(searchBox, text);

        submit(searchBox);

        waitForUrlContains("q=");

        return this;
    }

    public String getPageTitle() {

        return getTitle();
    }
}