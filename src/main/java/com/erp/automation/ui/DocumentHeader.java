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
 * Reads the header fields of an opened StudioERP document as field → shown
 * value: select text, input value, date, or the checked radio option.
 * The field ids are the data-testid without the screen prefix
 * (e.g. prefix "orders" + field "vendorId" → data-testid="orders-vendorId").
 */
public class DocumentHeader {

    private final WebDriver driver;
    private final String prefix;

    public DocumentHeader(WebDriver driver, String prefix) {

        this.driver = driver;
        this.prefix = prefix;
    }

    /**
     * Reads the header of the opened order as field → shown value
     * (select text, input value, date, or the checked "الأسعار" option).
     * A field that is not on the screen is returned as "".
     */
    @SuppressWarnings("unchecked")
    public Map<String, String> readHeaderValues(List<String> fields) {

        Map<String, String> raw = (Map<String, String>) ((JavascriptExecutor) driver).executeScript(
                "const out={};"
                        + "for (const f of arguments[0]) {"
                        + "  const e=document.querySelector(\"[data-testid='\"+arguments[1]+\"-\"+f+\"']\");"
                        + "  let v='';"
                        + "  if (e) {"
                        + "    if (e.tagName==='INPUT') v=e.value;"
                        + "    else if (e.getAttribute('role')==='radiogroup') {"
                        + "      const c=e.querySelector('[role=radio][aria-checked=true]');"
                        + "      const w=c && c.closest('div');"
                        + "      v=w ? w.innerText.trim() : '';"
                        + "    } else {"
                        + "      const sv=[...e.querySelectorAll('.select__single-value,.select__multi-value')];"
                        + "      const inp=e.querySelector('input[type=text]');"
                        + "      v=sv.length ? sv.map(x=>x.innerText.trim()).join(', ') : (inp ? inp.value : '');"
                        + "    }"
                        + "  }"
                        + "  out[f]=(v||'').trim();"
                        + "}"
                        + "return out;",
                fields, prefix);
        Map<String, String> ordered = new LinkedHashMap<>();
        fields.forEach(f -> ordered.put(f, raw.getOrDefault(f, "")));
        return ordered;
    }
}
