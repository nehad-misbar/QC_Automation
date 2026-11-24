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
import java.util.ArrayList;
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
                    + " or starts-with(normalize-space(),'PQ')"
                    // Sales (QC 2026-11-12: quotation cards show "SQ2608-000016" in span.text-xs)
                    + " or starts-with(normalize-space(),'SQ') or starts-with(normalize-space(),'SO')"
                    + " or starts-with(normalize-space(),'SI') or starts-with(normalize-space(),'SR')))"
                    // FQP cards hold the number in span.font-semibold (QC 2026-11-05)
                    + " or (contains(@class,'font-semibold') and starts-with(normalize-space(),'FQP'))]");
    /**
     * A Kanban card whatever it shows: the card button (rounded bordered column)
     * holding a status badge (span.rounded-full).
     */
    private final By kanbanCard = By.xpath(
            "//button[contains(@class,'flex-col') and contains(@class,'rounded-lg')]"
                    + "[.//span[contains(@class,'rounded-full')]]");
    // PO = orders, PR = requisitions (QC 2026-11-01: requisition cards show "PR2608-000001"),
    // PQ = price quotations, FQP = free quantity processing (QC 2026-11-05: the card shows "FQP2608-000003").
    private final By groupRow = By.cssSelector(".ag-center-cols-container div[role='row'].ag-row-group");
    private final By summariesToggle = By.xpath("//button[normalize-space()='الملخصات']");


    public DocumentList(WebDriver driver) {

        this.driver = driver;
    }

    /**
     * Opens the Kanban view and waits for its cards. It waits for the cards
     * themselves, not for a document number on them: on "فواتير المبيعات" the
     * cards show no number (QC 2026-11-16), and the old wait then ended in a
     * bare timeout instead of a clear assertion in the test.
     */
    public void switchToKanbanView() {

        new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.elementToBeClickable(kanbanViewButton)).click();
        new WebDriverWait(driver, Duration.ofSeconds(15))
                .until(d -> d.findElements(By.cssSelector(".ag-root")).isEmpty()
                        && (!d.findElements(kanbanCardCode).isEmpty() || !d.findElements(kanbanCard).isEmpty()));
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

    /**
     * One group of a grouped list: its label (the "المجموعة" column, e.g. "معتمد"),
     * the text its own row shows in the status column, and the statuses of its
     * documents shown once the group is expanded.
     */
    public record GroupStatus(String label, String rowStatus, List<String> childStatuses) { }

    /**
     * Opens the groups of the grouped list one at a time (as a user does) and
     * reads, for each group, the status its own row shows in column
     * {@code statusColumn} and the statuses of its documents; each group is
     * closed again before the next one is opened.
     *
     * <p>QC 2026-11-16 ("فواتير المبيعات" grouped by "حالة الفاتورة"): the group
     * is "معتمد (222)" and every invoice in it is معتمد, but the group row shows
     * "ملغي" in the "حالة الفاتورة" column.</p>
     *
     * <p>The grid renders only the columns in view, so it is scrolled sideways
     * until the group column, then the status column, is rendered.</p>
     */
    @SuppressWarnings("unchecked")
    public List<GroupStatus> getGroupStatuses(String statusColumn) {

        JavascriptExecutor js = (JavascriptExecutor) driver;
        new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.presenceOfElementLocated(groupRow));

        // 1. Group labels by row-id (needs the group column in view).
        scrollGridUntil("[...document.querySelectorAll('div[role=row].ag-row-group .ag-group-value')]"
                + ".some(v => v.textContent.trim())");
        Map<String, Object> labels = (Map<String, Object>) js.executeScript(
                "const out={};"
                        + "document.querySelectorAll('div[role=row].ag-row-group[row-id]').forEach(r=>{"
                        + "  const v=r.querySelector('.ag-group-value');"
                        + "  if(v && v.textContent.trim()) out[r.getAttribute('row-id')]=v.textContent.trim();});"
                        + "return out;");

        // 2. One group at a time. QC 2026-11-23 ("طلبات تعديل الأسعار", checked by hand): opening a
        //    group closes the group that is open, and that click does not open the clicked group.
        //    Expanding every group at once therefore left all of them closed, and the test reported
        //    "shows no documents when expanded" for groups that do show their documents.
        String statusVisible = "!!document.querySelector(\".ag-center-cols-container div[role=row] [col-id='"
                + statusColumn + "']\")";
        List<GroupStatus> groups = new ArrayList<>();
        for (Map.Entry<String, Object> group : labels.entrySet()) {
            String id = group.getKey();
            setGroupExpanded(id, true);
            scrollGridUntil(statusVisible);
            // Read until the group shows exactly its own count of documents, twice in a row (max 5 s).
            // QC 2026-11-23: read 5 ms after opening "معتمد (14)", the rows of the "مسودة" group just
            // closed were still in the grid, and the group seemed to hold 20 documents, 6 of them مسودة.
            int expected = groupChildCount(id);
            String rowStatus = "";
            List<String> children = new ArrayList<>();
            List<String> previous = null;
            long end = System.currentTimeMillis() + 5_000;
            while (true) {
                List<Map<String, Object>> rows = (List<Map<String, Object>>) js.executeScript(
                        "const col=arguments[0];"
                                + "return [...document.querySelectorAll('.ag-center-cols-container div[role=row][row-index]')]"
                                + "  .sort((a,b)=>Number(a.getAttribute('row-index'))-Number(b.getAttribute('row-index')))"
                                + "  .map(r=>{const c=r.querySelector(\"[col-id='\"+col+\"']\");"
                                + "    return {id:r.getAttribute('row-id')||'', group:r.classList.contains('ag-row-group'),"
                                + "            status:c ? c.textContent.trim() : ''};});",
                        statusColumn);
                children = new ArrayList<>();
                boolean inGroup = false;
                for (Map<String, Object> row : rows) {
                    if (Boolean.TRUE.equals(row.get("group"))) {
                        if (inGroup) {
                            break;
                        }
                        if (id.equals(String.valueOf(row.get("id")))) {
                            inGroup = true;
                            rowStatus = String.valueOf(row.get("status"));
                        }
                    } else if (inGroup) {
                        children.add(String.valueOf(row.get("status")));
                    }
                }
                boolean settled = children.equals(previous) && (expected < 0 || children.size() == expected);
                if (settled || System.currentTimeMillis() > end) {
                    break;
                }
                previous = children;
                pause(250);
            }
            groups.add(new GroupStatus(String.valueOf(group.getValue()), rowStatus, children));
            setGroupExpanded(id, false);
            waitForGroupDocumentsToLeave();
        }
        return groups;
    }

    /** The "(14)" count of group row {@code rowId}, or -1 when it is not shown. */
    private int groupChildCount(String rowId) {
        Object count = ((JavascriptExecutor) driver).executeScript(
                "const c=[...document.querySelectorAll(\"div[role=row][row-id='\" + arguments[0] + \"'] .ag-group-child-count\")]"
                        + ".map(e=>e.textContent.replace(/[^0-9]/g,'')).find(t=>t);"
                        + "return c ? Number(c) : -1;", rowId);
        return count instanceof Number n ? n.intValue() : -1;
    }

    /** After a group is closed: waits (max 5 s) until no document row of it is left in the grid. */
    private void waitForGroupDocumentsToLeave() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(5)).pollingEvery(Duration.ofMillis(200))
                    .until(d -> Boolean.TRUE.equals(((JavascriptExecutor) d).executeScript(
                            "return [...document.querySelectorAll('.ag-center-cols-container div[role=row][row-index]')]"
                                    + ".every(r => r.classList.contains('ag-row-group'));")));
        } catch (TimeoutException stillThere) {
            System.out.println("Documents of a closed group are still shown after 5 s.");
        }
    }

    /** Whether group row {@code rowId} is expanded. */
    private boolean isGroupExpanded(String rowId) {
        return Boolean.TRUE.equals(((JavascriptExecutor) driver).executeScript(
                "return !!document.querySelector(\"div[role=row][row-id='\" + arguments[0] + \"'].ag-row-group-expanded\");",
                rowId));
    }

    /**
     * Opens (or closes) group row {@code rowId} with a click on its visible chevron, and waits
     * until it is. Up to 3 clicks: the first click on a closed group may only close another one.
     */
    private void setGroupExpanded(String rowId, boolean expanded) {

        JavascriptExecutor js = (JavascriptExecutor) driver;
        for (int attempt = 1; attempt <= 3; attempt++) {
            if (isGroupExpanded(rowId) == expanded) {
                return;
            }
            scrollGridUntil("[...document.querySelectorAll(\"div[role=row][row-id='" + rowId
                    + "'] .ag-group-contracted, div[role=row][row-id='" + rowId
                    + "'] .ag-group-expanded\")].some(t => t.offsetParent !== null)");
            WebElement chevron = (WebElement) js.executeScript(
                    "return [...document.querySelectorAll(\"div[role=row][row-id='\" + arguments[0] + \"'] .ag-group-contracted,"
                            + " div[role=row][row-id='\" + arguments[0] + \"'] .ag-group-expanded\")]"
                            + ".find(t => t.offsetParent !== null) || null;", rowId);
            try {
                chevron.click();
            } catch (ElementClickInterceptedException | StaleElementReferenceException notClickable) {
                js.executeScript("arguments[0].click();", chevron);
            }
            try {
                new WebDriverWait(driver, Duration.ofSeconds(3)).until(d -> isGroupExpanded(rowId) == expanded);
                return;
            } catch (TimeoutException notYet) {
                // the click closed another group first; click again
            }
        }
        throw new TimeoutException("Group row " + rowId + " did not " + (expanded ? "open" : "close") + " after 3 clicks.");
    }

    /**
     * Scrolls the list grid sideways (both directions, from the start) until the
     * JavaScript {@code condition} is true.
     */
    private void scrollGridUntil(String condition) {

        JavascriptExecutor js = (JavascriptExecutor) driver;
        String check = "return !!(" + condition + ");";
        if (Boolean.TRUE.equals(js.executeScript(check))) {
            return;
        }
        for (int direction : new int[]{-1, 1}) {
            js.executeScript("const v=document.querySelector('.ag-center-cols-viewport'); if (v) v.scrollLeft=0;");
            for (int step = 0; step < 30; step++) {
                Object moved = js.executeScript(
                        "const v=document.querySelector('.ag-center-cols-viewport'); if (!v) return false;"
                                + "const before=v.scrollLeft; v.scrollLeft=before+arguments[0]*250;"
                                + "return v.scrollLeft!==before;", direction);
                pause(250);
                if (Boolean.TRUE.equals(js.executeScript(check))) {
                    return;
                }
                if (!Boolean.TRUE.equals(moved)) {
                    break;
                }
            }
        }
        throw new TimeoutException("List grid: not found after scrolling sideways: " + condition);
    }

    private static void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
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
