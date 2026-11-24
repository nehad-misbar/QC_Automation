package com.erp.automation.reporting;

import com.aventstack.extentreports.ExtentTest;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Copies every console line written while a test runs into that test's
 * Extent entry, so the report shows the same steps as the console/IntelliJ
 * log (page-object {@code System.out} messages and SLF4J/logback lines).
 *
 * <p>The original console output is unchanged. Lines are attached to the
 * test bound to the CURRENT thread; lines written when no test is active
 * (suite start, {@code @BeforeMethod} before {@code onTestStart}) only go to
 * the console.</p>
 *
 * <p>Stack traces are NOT copied: a {@code logger.error("…", throwable)}
 * used to add every "at …" line as a separate Info step, and the listener
 * then showed the same exception again in the Fail block. The exception is
 * shown once, by the listener. Log lines at ERROR / WARN level are coloured
 * so they no longer look like plain Info steps (the step status stays Info,
 * so a logged error does not change the test result).</p>
 */
final class ConsoleToReportStream extends OutputStream {

    /** Very long lines (e.g. grid dumps) are cut to keep the HTML light. */
    private static final int MAX_LINE_LENGTH = 1000;

    /** A logback line: {@code HH:mm:ss.SSS LEVEL [thread] logger - message} (see logback.xml). */
    private static final Pattern LOG_LINE = Pattern.compile(
            "^\\d{2}:\\d{2}:\\d{2}\\.\\d{3}\\s+(TRACE|DEBUG|INFO|WARN|ERROR)\\s+\\[.*");

    /** First line of a printed exception: {@code a.b.SomeException} or {@code a.b.SomeException: message}. */
    private static final Pattern EXCEPTION_HEADER = Pattern.compile(
            "^(?:[\\w$]+\\.)+[\\w$]*(?:Exception|Error|Throwable)(?::.*)?$");

    /**
     * Safety limit: lines dropped as the message of an exception before its
     * first "at …" frame (an exception printed without frames must not hide
     * the rest of the test's steps).
     */
    private static final int MAX_EXCEPTION_MESSAGE_LINES = 40;

    /** Where the current thread is inside a printed stack trace. */
    private enum TraceState { NONE, AFTER_ERROR_LOG, MESSAGE, FRAMES }

    private final PrintStream original;
    private final ThreadLocal<ByteArrayOutputStream> lineBuffer =
            ThreadLocal.withInitial(ByteArrayOutputStream::new);
    private final ThreadLocal<TraceState> traceState =
            ThreadLocal.withInitial(() -> TraceState.NONE);
    private final ThreadLocal<int[]> messageLines =
            ThreadLocal.withInitial(() -> new int[1]);

    private ConsoleToReportStream(PrintStream original) {
        this.original = original;
    }

    /** Replaces System.out once; later calls do nothing. */
    static synchronized void install() {

        if (System.out instanceof TeePrintStream) {
            return;
        }
        PrintStream console = System.out;
        // Same charset as the console, so the console output is byte-identical.
        System.setOut(new TeePrintStream(new ConsoleToReportStream(console), console.charset()));
    }

    @Override
    public void write(int b) {

        original.write(b);

        if (b == '\n') {
            flushLine();
        } else if (b != '\r') {
            lineBuffer.get().write(b);
        }
    }

    @Override
    public void write(byte[] bytes, int offset, int length) {
        for (int i = offset; i < offset + length; i++) {
            write(bytes[i]);
        }
    }

    @Override
    public void flush() {
        original.flush();
    }

    private void flushLine() {

        ByteArrayOutputStream buffer = lineBuffer.get();
        String line = buffer.toString(original.charset()).strip();
        buffer.reset();

        if (line.isEmpty()) {
            return;
        }

        // Track stack traces even when no test is active, so the state is right
        // for the next line.
        String level = null;
        Matcher log = LOG_LINE.matcher(line);
        if (log.matches()) {
            level = log.group(1);
            traceState.set("ERROR".equals(level) || "WARN".equals(level)
                    ? TraceState.AFTER_ERROR_LOG : TraceState.NONE);
        } else if (isStackTraceLine(line)) {
            return;
        }

        ExtentTest test = ExtentReportManager.currentTestOrNull();
        if (test == null) {
            return;
        }

        if (line.length() > MAX_LINE_LENGTH) {
            line = line.substring(0, MAX_LINE_LENGTH) + " …";
        }

        String html = escapeHtml(line);
        if ("ERROR".equals(level)) {
            html = "<span style=\"color:#dc3545;font-weight:600\">" + html + "</span>";
        } else if ("WARN".equals(level)) {
            html = "<span style=\"color:#e0a800;font-weight:600\">" + html + "</span>";
        }

        try {
            test.info(html);
        } catch (RuntimeException ignored) {
            // never let reporting break the test run
        }
    }

    /**
     * True for a (non-logback) line that belongs to an exception printed after an
     * ERROR / WARN log line: its header, its message lines, "at …" frames,
     * "Caused by:", "Suppressed:" and "... n more" / "... n common frames omitted".
     * Any other line ends the trace and is reported normally.
     */
    private boolean isStackTraceLine(String line) {

        switch (traceState.get()) {
            case AFTER_ERROR_LOG:
                if (EXCEPTION_HEADER.matcher(line).matches()) {
                    startMessage();
                    return true;
                }
                traceState.set(TraceState.NONE);
                return false;

            case MESSAGE:
                if (line.startsWith("at ")) {
                    traceState.set(TraceState.FRAMES);
                    return true;
                }
                if (++messageLines.get()[0] > MAX_EXCEPTION_MESSAGE_LINES) {
                    traceState.set(TraceState.NONE);
                    return false;
                }
                return true;   // multi-line exception message (e.g. Selenium's Build info / Capabilities)

            case FRAMES:
                if (line.startsWith("Caused by:") || line.startsWith("Suppressed:")) {
                    startMessage();
                    return true;
                }
                if (line.startsWith("at ") || line.startsWith("... ")) {
                    return true;
                }
                traceState.set(TraceState.NONE);
                return false;

            default:
                return false;
        }
    }

    private void startMessage() {
        traceState.set(TraceState.MESSAGE);
        messageLines.get()[0] = 0;
    }

    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    /** Marker type so {@link #install()} is idempotent. */
    private static final class TeePrintStream extends PrintStream {
        TeePrintStream(OutputStream out, java.nio.charset.Charset charset) {
            super(out, true, charset);
        }
    }
}
