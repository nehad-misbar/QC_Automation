package com.erp.automation.ui;

import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Records the page's non-GET API calls (method, URL, status, start of the
 * answer) so a test can judge an action by the server's answer, not only by
 * the toast. QC 2026-10-31: a cancel showed "تم الحفظ بنجاح." while the API
 * had answered 400.
 */
public class ApiRecorder {

    private final WebDriver driver;

    public ApiRecorder(WebDriver driver) {

        this.driver = driver;
    }

    /**
     * Starts recording the page's non-GET API calls (method, URL, status and
     * the start of the response) in window.__qaApiCalls. Call it right before an
     * action whose server answer matters, then read {@link #getRecordedApiCalls()}.
     *
     * <p>QC, 2026-10-31: during a Selenium run, cancelling an approved order
     * showed "نجاح / تم الحفظ بنجاح." while the order stayed Approved, and doing
     * the same by hand returned 400 "لايمكن الغاء امر شراء مرتبط ب امر استلام".
     * The toast alone is not enough evidence, so the API answer is recorded.</p>
     */
    public void startRecordingApiCalls() {

        ((JavascriptExecutor) driver).executeScript(
                "window.__qaApiCalls = [];"
                        + "if (!window.__qaApiHooked) {"
                        + "  window.__qaApiHooked = true;"
                        + "  const of = window.fetch;"
                        + "  window.fetch = async function(...a) {"
                        + "    const r = await of.apply(this, a);"
                        + "    try {"
                        + "      const m = (a[1] && a[1].method) || (a[0] && a[0].method) || 'GET';"
                        + "      if (m !== 'GET') { const t = await r.clone().text();"
                        + "        window.__qaApiCalls.push(m + ' ' + String((a[0] && a[0].url) || a[0]) + ' -> ' + r.status + ' ' + t.slice(0, 300)); }"
                        + "    } catch (e) {}"
                        + "    return r;"
                        + "  };"
                        + "  const oo = XMLHttpRequest.prototype.open, os = XMLHttpRequest.prototype.send;"
                        + "  XMLHttpRequest.prototype.open = function(m, u) { this.__qaM = m; this.__qaU = u; return oo.apply(this, arguments); };"
                        + "  XMLHttpRequest.prototype.send = function(b) {"
                        + "    this.addEventListener('loadend', () => { if (this.__qaM !== 'GET')"
                        + "      window.__qaApiCalls.push(this.__qaM + ' ' + this.__qaU + ' -> ' + this.status + ' '"
                        + "        + String(this.responseText || '').slice(0, 300)); });"
                        + "    return os.apply(this, arguments);"
                        + "  };"
                        + "}");
    }

    /** Non-GET API calls recorded since {@link #startRecordingApiCalls()}. */
    @SuppressWarnings("unchecked")
    public List<String> getRecordedApiCalls() {

        Object calls = ((JavascriptExecutor) driver).executeScript("return window.__qaApiCalls || [];");
        return calls instanceof List<?> list
                ? list.stream().map(String::valueOf).toList()
                : List.of();
    }
}
