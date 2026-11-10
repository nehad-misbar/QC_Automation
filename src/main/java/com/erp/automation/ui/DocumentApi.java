package com.erp.automation.ui;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Predicate;

/**
 * Reads a screen's records through the application's own API, from inside
 * the logged-in browser.
 *
 * <p>Why: the list shows the status ("الحالة"), but not everything a
 * scenario may need, for example whether a requisition is linked to a
 * quotation or an invoice. The API returns those flags:
 * {@code recordStatus}, {@code isApproved}, {@code isCanceled},
 * {@code isInvoice}, {@code totalQutotaionCount}, and so on.</p>
 *
 * <p>How: the request runs in the page with {@code fetch}. It uses the
 * bearer token the app keeps in the {@code authToken} cookie, so no
 * credentials are handled by the test. The API host is the one the page
 * itself calls (read from the browser's resource timings), so nothing is
 * hard-coded per environment.</p>
 *
 * <pre>
 * DocumentApi api = new DocumentApi(driver, "PurchasRequisitions");
 * Map&lt;String, Object&gt; draft = api.randomRecord(r -&gt; api.intOf(r, "recordStatus") == 0);
 * </pre>
 */
public class DocumentApi {

    /** Records read per request; every Purchasing screen has far fewer in QC. */
    private static final int PAGE_SIZE = 1000;

    private final WebDriver driver;
    private final String resource;
    private final Random random = new Random();

    /**
     * @param resource API resource name as the app spells it,
     *                 e.g. {@code PurchasRequisitions}. When the screen's
     *                 filter endpoint has an extra part, pass the whole path:
     *                 local purchase invoices read
     *                 {@code PurchasInvoice/filter/0} (QC 2026-11-05;
     *                 {@code PurchasInvoice/filter} answers 405).
     */
    public DocumentApi(WebDriver driver, String resource) {
        this.driver = driver;
        this.resource = resource;
    }

    /** All records of the screen, as returned by {@code POST /api/<resource>/filter}. */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> findAll() {

        Object result = ((JavascriptExecutor) driver).executeAsyncScript(
                "const [resource, take, done] = arguments;"
                        + "const cookie = document.cookie.split(';').map(c => c.trim())"
                        + "  .find(c => c.startsWith('authToken='));"
                        + "if (!cookie) { done('ERROR: no authToken cookie (not logged in?)'); return; }"
                        + "const token = decodeURIComponent(cookie.substring('authToken='.length));"
                        + "const api = performance.getEntriesByType('resource').map(e => e.name)"
                        + "  .map(n => n.match(/^(https?:\\/\\/[^/]+\\/api)\\//)).find(m => m);"
                        + "if (!api) { done('ERROR: API host not found in resource timings'); return; }"
                        + "const body = {skip: 0, take: take, columnSettings: [],"
                        + "  distinctField: {aggregation: 0, dataType: '', fieldName: ''},"
                        + "  filters: [], multiSelectField: [],"
                        + "  sorting: {fieldName: '', isDescending: false}, searchTerm: ''};"
                        + "fetch(api[1] + '/' + resource, {method: 'POST',"
                        + "  headers: {'Content-Type': 'application/json', 'Authorization': 'Bearer ' + token},"
                        + "  body: JSON.stringify(body)})"
                        + ".then(r => r.ok ? r.json() : Promise.reject('HTTP ' + r.status))"
                        + ".then(j => done(((j.data || {}).data || {}).data || []))"
                        + ".catch(e => done('ERROR: ' + e));",
                filterPath(), PAGE_SIZE);

        if (result instanceof String error) {
            throw new IllegalStateException("Reading " + resource + " through the API failed: " + error);
        }
        return new ArrayList<>((List<Map<String, Object>>) result);
    }

    /** {@code <resource>/filter}, or {@code resource} itself when it already names the filter path. */
    private String filterPath() {
        return resource.contains("/filter") ? resource : resource + "/filter";
    }

    /** Records matching {@code filter}. */
    public List<Map<String, Object>> find(Predicate<Map<String, Object>> filter) {
        return findAll().stream().filter(filter).toList();
    }

    /** A random record matching {@code filter}, or {@code null} when there is none. */
    public Map<String, Object> randomRecord(Predicate<Map<String, Object>> filter) {

        List<Map<String, Object>> matches = find(filter);
        return matches.isEmpty() ? null : matches.get(random.nextInt(matches.size()));
    }

    /** The record with this document number, or {@code null}. */
    public Map<String, Object> byCode(String code) {
        return find(r -> code.equals(r.get("code"))).stream().findFirst().orElse(null);
    }

    /** Number of records the API reports. */
    public int count() {
        return findAll().size();
    }

    // ----- typed readers (JSON numbers arrive as Long or Double) -----

    public static int intOf(Map<String, Object> record, String field) {
        Object value = record.get(field);
        return value instanceof Number n ? n.intValue() : -1;
    }

    public static boolean isTrue(Map<String, Object> record, String field) {
        return Boolean.TRUE.equals(record.get(field));
    }

    public static String textOf(Map<String, Object> record, String field) {
        Object value = record.get(field);
        return value == null ? "" : value.toString();
    }
}
