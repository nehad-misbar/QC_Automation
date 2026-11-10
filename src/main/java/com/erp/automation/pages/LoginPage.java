package com.erp.automation.pages;

import com.erp.automation.utils.LocatorUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

public class LoginPage extends BasePage {

    private final By usernameField =
            LocatorUtils.testId("-username");

    private final By passwordField =
            LocatorUtils.testId("-password");

    private final By loginButton =
            LocatorUtils.xpath("//button[normalize-space()='تسجيل الدخول']");

    /**
     * "يوجد جلسة نشطة لهذا الحساب" dialog (QC 2026-11-05): shown when the same user is
     * signed in elsewhere. Its button ends the other session and signs in here.
     */
    private final By endSessionAndLoginButton =
            By.xpath("//button[contains(normalize-space(),'إنهاء الجلسة') or contains(normalize-space(),'انهاء الجلسة')]");

    /** How long to wait for that dialog after "تسجيل الدخول" (it does not always appear). */
    private static final Duration ACTIVE_SESSION_WAIT = Duration.ofSeconds(5);

    public LoginPage(WebDriver driver) {
        super(driver);
    }

    public LoginPage open(String url) {
        navigateTo(url);
        return this;
    }

    public LoginPage enterUsername(String username) {
        type(usernameField, username);
        return this;
    }

    public LoginPage enterPassword(String password) {
        type(passwordField, password);
        return this;
    }

    public LoginPage clickLogin() {
        click(loginButton);
        return this;
    }

    /**
     * Clicks "إنهاء الجلسة والدخول" when the active-session dialog appears (waits up to
     * 5 s for it); does nothing when it does not appear.
     *
     * @return true when the dialog was shown and the button was clicked.
     */
    public boolean endActiveSessionIfAsked() {
        WebElement button;
        try {
            button = new WebDriverWait(driver, ACTIVE_SESSION_WAIT)
                    .pollingEvery(Duration.ofMillis(250))
                    .until(d -> {
                        List<WebElement> found = d.findElements(endSessionAndLoginButton);
                        return found.stream().filter(WebElement::isDisplayed).findFirst().orElse(null);
                    });
        } catch (org.openqa.selenium.TimeoutException noDialog) {
            return false;
        }
        try {
            button.click();
        } catch (RuntimeException covered) {
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", button);
        }
        new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(d -> d.findElements(endSessionAndLoginButton).stream().noneMatch(WebElement::isDisplayed));
        return true;
    }

    public boolean isLoginButtonEnabled() {
        return isEnabled(loginButton);
    }

    public boolean isUsernameDisplayed() {
        return isDisplayed(usernameField);
    }

    public boolean isPasswordDisplayed() {
        return isDisplayed(passwordField);
    }

    public LoginPage login(String username, String password) {
        endActiveSessionIfAsked(); // the dialog can already be open on the login page
        enterUsername(username);
        enterPassword(password);
        clickLogin();
        endActiveSessionIfAsked(); // or appear after "تسجيل الدخول"
        return this;
    }
}
