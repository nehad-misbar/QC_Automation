package com.erp.automation.data;

import java.util.Arrays;

/**
 * Business status shown in the "الحالة" column of every Purchasing grid
 * (Requisitions, Quotations, Orders, Invoices, Returns).
 *
 * <p>Use it instead of comparing raw Arabic strings in tests:</p>
 * <pre>
 * DocumentStatus status = DocumentStatus.fromLabel(page.selectRandom...AndGetStatus());
 * requireStatus(status, DocumentStatus.DRAFT, DocumentStatus.APPROVED);
 * </pre>
 */
public enum DocumentStatus {

    DRAFT("مسودة"),
    APPROVED("معتمد"),
    COMPLETED("منتهي"),
    CANCELLED("ملغي");

    private final String label;

    DocumentStatus(String label) {
        this.label = label;
    }

    /** The Arabic text exactly as displayed in the grid. */
    public String label() {
        return label;
    }

    public boolean isOneOf(DocumentStatus... statuses) {
        return Arrays.asList(statuses).contains(this);
    }

    /**
     * Converts the grid text into a status.
     *
     * @throws IllegalArgumentException for an unknown label, so a broken
     *         status reader fails loudly instead of being silently ignored.
     */
    public static DocumentStatus fromLabel(String label) {

        String normalized = label == null ? "" : label.trim();

        return Arrays.stream(values())
                .filter(status -> status.label.equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown document status: [" + label + "]"
                ));
    }

    @Override
    public String toString() {
        return name() + "(" + label + ")";
    }
}
