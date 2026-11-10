package com.erp.automation.reporting;

import com.aventstack.extentreports.ExtentTest;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * Copies every console line written while a test runs into that test's
 * Extent entry, so the report shows the same steps as the console/IntelliJ
 * log (page-object {@code System.out} messages and SLF4J/logback lines).
 *
 * <p>The original console output is unchanged. Lines are attached to the
 * test bound to the CURRENT thread; lines written when no test is active
 * (suite start, {@code @BeforeMethod} before {@code onTestStart}) only go to
 * the console.</p>
 */
final class ConsoleToReportStream extends OutputStream {

    /** Very long lines (e.g. grid dumps) are cut to keep the HTML light. */
    private static final int MAX_LINE_LENGTH = 1000;

    private final PrintStream original;
    private final ThreadLocal<ByteArrayOutputStream> lineBuffer =
            ThreadLocal.withInitial(ByteArrayOutputStream::new);

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

        ExtentTest test = ExtentReportManager.currentTestOrNull();
        if (test == null) {
            return;
        }

        if (line.length() > MAX_LINE_LENGTH) {
            line = line.substring(0, MAX_LINE_LENGTH) + " …";
        }

        try {
            test.info(escapeHtml(line));
        } catch (RuntimeException ignored) {
            // never let reporting break the test run
        }
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
