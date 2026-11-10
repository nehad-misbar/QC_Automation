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
 * The list screen of a StudioERP document (Purchase Orders, Requisitions,
 * Quotations, Invoices …): pagination summary, Kanban view, "تجميع حسب"
 * (group by) and "الملخصات" (summary cards). The toolbar is the same on every
 * document list, so the locators do not depend on the screen.
 */
public class DocumentList {

    private final WebDriver driver;

    // The list toolbar buttons have generated data-testids (orders-_r_1g_ ...)
    // that change between builds, so they are found by their lucide icon:
    //   list  = two stacked rects (20x6), kanban = rect 3x9 inside a square,
    //   filter = funnel polygon "22 3 2 3 ...".

    private final By listViewButton = By.xpath(
            "//button[.//*[local-name()='rect' and @width='20' and @height='6']]");
    private final By kanbanViewButton = By.xpath(
            "//button[.//*[local-name()='rect' and @width='3' and @height='9']]");
    private final By listFilterButton = By.xpath(
            "//button[.//*[local-name()='polygon' and starts-with(@points,'22 3 2 3')]]");
    private final By groupBySelect = By.xpath(
            "//div[contains(@class,'select__control')]"
                    + "[.//*[local-name()='svg' and contains(@class,'lucide-separator-vertical')]]");
    /** A Kanban card: a button holding the order number in span.text-xs. */
    private final By kanbanCardCode = By.xpath(
            "//button//span[(contains(@class,'text-xs')"
                    + " and (starts-with(normalize-space(),'PO') or starts-with(normalize-space(),'PR')"
                    + " or starts-with(normalize-space(),'PQ')))"
                    // FQP cards hold the number in span.font-semibold (QC 2026-11-05)
                    + " or (contains(@class,'font-semibold') and starts-with(normalize-space(),'FQP'))]");
    // PO = orders, PR = requisitions (QC 2026-11-01: requisition cards show "PR2608-000001"),
    // PQ = price quotations, FQP = free quantity processing (QC 2026-11-05: the card shows "FQP2608-000003").
    private final By groupRow = By.cssSelector(".ag-center-cols-container div[role='row'].ag-row-group");
    private final By summariesToggle = By.xpath("//button[normalize-space()='الملخصات']");


    public DocumentList(WebDriver driver) {

        this.driver = driver;
    }

    public void switchToKanbanView() {

        new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.elementToBeClickable(kanbanViewButton)).click();
        new WebDriverWait(driver, Duration.ofSeconds(15))
                .until(d -> d.findElements(By.cssSelector(".ag-root")).isEmpty()
                        && !d.findElements(kanbanCardCode).isEmpty());
    }

    public void switchToListView() {

        new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.elementToBeClickable(listViewButton)).click();
        new WebDriverWait(driver, Duration.ofSeconds(15))
                .until(ExpectedConditions.visibilityOfElementLocated(
                        By.cssSelector(".ag-center-cols-container div[role='row']")));
    }

    /** Order numbers on the Kanban cards, in screen order. */
    public List<String> getKanbanCardCodes() {

        return new WebDriverWait(driver, Duration.ofSeconds(10))
                .ignoring(StaleElementReferenceException.class)
                .until(d -> d.findElements(kanbanCardCode).stream()
                        .map(e -> e.getText().trim()).distinct().toList());
    }

    /** Status shown on the Kanban card of {@code code}. */
    public String getKanbanCardStatus(String code) {

        WebElement card = driver.findElement(By.xpath(
                "//button[.//span[(contains(@class,'text-xs') or contains(@class,'font-semibold')) and normalize-space()='" + code + "']]"));
        return card.findElement(By.xpath(".//span[contains(@class,'rounded-full')]")).getText().trim();
    }

    /**
     * Opens the Kanban card of {@code code}. QC 2026-11-09 (free quantity processing): a
     * double-click on the card did not open it, so the card's menu → "عرض" is used when the
     * double-click has not opened the document within 3 s.
     */
    public void openKanbanCard(String code) {

        By cardLocator = By.xpath("//button[.//span[(contains(@class,'text-xs') or contains(@class,'font-semibold'))"
                + " and normalize-space()='" + code + "']]");
        WebElement card = driver.findElement(cardLocator);
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", card);
        new Actions(driver).doubleClick(card).perform();
        if (waitForView(Duration.ofSeconds(3))) {
            return;
        }
        System.out.println("Kanban card " + code + ": double-click did not open it → menu → عرض");
        openKanbanCardFromMenu(driver.findElement(cardLocator));
        if (!waitForView(Duration.ofSeconds(15))) {
            throw new TimeoutException("Kanban card " + code + " did not open (double-click, then menu → عرض). URL: "
                    + driver.getCurrentUrl());
        }
    }

    private static final By VIEW_MENU_ITEM = By.xpath(
            "//*[@role='menuitem' or self::button or self::a or self::li]"
                    + "[normalize-space()='عرض' or .//*[normalize-space()='عرض']]"
                    + "[ancestor-or-self::*[@role='menu' or @data-state='open' or contains(@class,'dropdown')"
                    + " or contains(@class,'popover') or contains(@class,'menu')]]");

    /**
     * The card's own menu button (⋮ / …, outside the card button itself: a button cannot hold
     * another), else a right-click on the card; then "عرض".
     */
    private void openKanbanCardFromMenu(WebElement card) {

        WebElement trigger = (WebElement) ((JavascriptExecutor) driver).executeScript(
                "const card = arguments[0];"
                        + "const isTrigger = b => b !== card && !card.contains(b) && b.offsetParent !== null && ("
                        + "  b.getAttribute('aria-haspopup') === 'menu' || b.getAttribute('aria-haspopup') === 'true'"
                        + "  || !!b.querySelector('svg[class*=ellipsis], svg[class*=more-vertical], svg[class*=more-horizontal]'));"
                        + "const inside = [...card.querySelectorAll('[aria-haspopup], [role=button]')]"
                        + "  .find(b => b.offsetParent !== null && (b.getAttribute('aria-haspopup')"
                        + "   || b.querySelector('svg[class*=ellipsis], svg[class*=more-vertical], svg[class*=more-horizontal]')));"
                        + "if (inside) return inside;"
                        + "let box = card.parentElement;"
                        + "for (let i = 0; i < 4 && box; i++, box = box.parentElement) {"
                        + "  const cards = box.querySelectorAll('button span.font-semibold, button span.text-xs');"
                        + "  const found = [...box.querySelectorAll('button')].find(isTrigger);"
                        + "  if (found) return found;"
                        + "  if (cards.length > 1) break;" // reached the board: stop before another card's menu
                        + "}"
                        + "return null;",
                card);
        if (trigger != null) {
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", trigger);
            new Actions(driver).moveToElement(card).perform(); // menus that show on hover
            try {
                trigger.click();
            } catch (ElementClickInterceptedException covered) {
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", trigger);
            }
        } else {
            new Actions(driver).contextClick(card).perform();
        }
        WebElement view = new WebDriverWait(driver, Duration.ofSeconds(5))
                .ignoring(StaleElementReferenceException.class)
                .until(d -> d.findElements(VIEW_MENU_ITEM).stream().filter(WebElement::isDisplayed)
                        .reduce((first, second) -> second).orElse(null)); // the innermost match
        view.click();
    }

    private boolean waitForView(Duration timeout) {
        try {
            new WebDriverWait(driver, timeout)
                    .until(ExpectedConditions.urlContains("/view/")); // any screen (orders, requisitions)
            return true;
        } catch (TimeoutException notYet) {
            return false;
        }
    }

    /** Filter (funnel) → "تجميع حسب..." → {@code field}; closes the panel again. */
    public void groupListBy(String field) {

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.elementToBeClickable(listFilterButton)).click();
        WebElement control = wait.until(ExpectedConditions.elementToBeClickable(groupBySelect));
        control.click();
        WebElement input = control.findElement(By.xpath(".//input[@role='combobox']"));
        WebElement option = wait.until(d -> {
            String listId = input.getAttribute("aria-controls");
            By options = listId == null
                    ? By.cssSelector("div[class*='select__option']")
                    : By.cssSelector("#" + listId + " div[class*='select__option']");
            return d.findElements(options).stream()
                    .filter(o -> o.getText().trim().equals(field)).findFirst().orElse(null);
        });
        option.click();
        new Actions(driver).sendKeys(Keys.ESCAPE).perform();
        new WebDriverWait(driver, Duration.ofSeconds(15))
                .until(ExpectedConditions.presenceOfElementLocated(groupRow));
        System.out.println("List grouped by: " + field);
    }

    /**
     * Group rows of the list as label → count, e.g. {معتمد=72, ملغي=23}.
     *
     * <p>Read with textContent by row-id: in the wide Selenium window the label
     * and the "(72)" can sit in different (pinned/center) containers, and
     * getText() returned "" for labels that were scrolled out of view
     * (QC, 2026-10-31: {=202, منتهي=4}).</p>
     */
    @SuppressWarnings("unchecked")
    public Map<String, Integer> getGroupCounts() {

        new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.presenceOfElementLocated(groupRow));
        Map<String, Object> raw = (Map<String, Object>) ((JavascriptExecutor) driver).executeScript(
                "const byId={};"
                        + "document.querySelectorAll('div[role=row].ag-row-group[row-id]').forEach(r=>{"
                        + "  const id=r.getAttribute('row-id'); const g=byId[id]||(byId[id]={label:'',count:''});"
                        + "  const v=r.querySelector('.ag-group-value'); const c=r.querySelector('.ag-group-child-count');"
                        + "  if(v && v.textContent.trim()) g.label=v.textContent.trim();"
                        + "  if(c && c.textContent.trim()) g.count=c.textContent.replace(/[^0-9]/g,'');"
                        + "});"
                        + "const out={}; Object.values(byId).forEach(g=>{ if(g.count) out[g.label]=Number(g.count); });"
                        + "return out;");
        Map<String, Integer> groups = new LinkedHashMap<>();
        raw.forEach((label, count) -> groups.put(label, ((Number) count).intValue()));
        return groups;
    }

    /** Opens "الملخصات" (if closed) and returns each card as status → number ("208+" → 208). */
    public Map<String, Integer> getSummaryCards() {

        WebElement toggle = new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.elementToBeClickable(summariesToggle));
        if (!"true".equals(toggle.getAttribute("aria-expanded"))) {
            toggle.click();
        }
        // The cards render one by one: in the mvn run (2026-10-31) only 2 of the
        // 4 were read. Read until the same cards are seen for 1 s (max 10 s).
        By card = By.xpath("//div[contains(@class,'bg-card')][.//h3][.//p]");
        Map<String, Integer> last = new LinkedHashMap<>();
        long since = System.currentTimeMillis();
        long end = since + 10_000;
        while (System.currentTimeMillis() < end) {
            Map<String, Integer> now = new LinkedHashMap<>();
            try {
                for (WebElement c : driver.findElements(card)) {
                    String label = c.findElement(By.tagName("h3")).getText().trim();
                    String number = c.findElement(By.tagName("p")).getText().replaceAll("[^0-9]", "");
                    if (!label.isEmpty() && !number.isEmpty()) {
                        now.put(label, Integer.parseInt(number));
                    }
                }
            } catch (StaleElementReferenceException rerendered) {
                now = null;
            }
            if (now == null || now.isEmpty() || !now.equals(last)) {
                last = now == null ? new LinkedHashMap<>() : now;
                since = System.currentTimeMillis();
            } else if (System.currentTimeMillis() - since >= 1_000) {
                return now;
            }
            UiWaits.pollPause();
        }
        if (last.isEmpty()) {
            throw new TimeoutException("No summary cards (الملخصات) were shown within 10 s.");
        }
        return last;
    }

    /**
     * Reads the pagination summary "عرض {from} - {to} / {total}".
     *
     * @return {from, to, total}
     * @throws IllegalStateException if the summary is not displayed.
     */
    public int[] getPaginationRange() {

        Object text = ((JavascriptExecutor) driver).executeScript(
                "const m=document.body.innerText.match("
                        + "/\\u0639\\u0631\\u0636\\s*([\\d,]+)\\s*-\\s*([\\d,]+)\\s*\\/\\s*([\\d,]+)/);"
                        + "return m ? [m[1],m[2],m[3]].map(x=>x.replace(/,/g,'')).join(',') : null;");

        if (text == null) {
            throw new IllegalStateException("Pagination summary (عرض x - y / z) is not displayed.");
        }

        String[] parts = text.toString().split(",");
        return new int[]{
                Integer.parseInt(parts[0]),
                Integer.parseInt(parts[1]),
                Integer.parseInt(parts[2])
        };
    }
}
