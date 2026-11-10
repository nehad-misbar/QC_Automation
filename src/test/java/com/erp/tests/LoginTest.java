package com.erp.tests;

import com.erp.automation.base.BaseTest;
import com.erp.automation.config.ConfigReader;
import com.erp.automation.pages.LoginPage;
import org.testng.Assert;
import org.testng.annotations.Test;

public class LoginTest extends BaseTest {

    @Test(groups = "smoke")
    public void loginButtonShouldBeEnabled() {

        LoginPage loginPage = new LoginPage(driver);

        loginPage.open(
                "https://qc.studioerp.com/login"
        );

        Assert.assertTrue(
                loginPage.isLoginButtonEnabled(),
                "Login button should be enabled."
        );
    }

    @Test(groups = "smoke")
    public void userCanLoginSuccessfully() {

        LoginPage loginPage = new LoginPage(driver);

        loginPage.open(
                "https://qc.studioerp.com"
        );

        loginPage.login(
                ConfigReader.get("username"),
                ConfigReader.get("password")
        );

        System.out.println(
                "URL after login: " + driver.getCurrentUrl()
        );

        System.out.println(
                "Title after login: " + driver.getTitle()
        );
    }
}

