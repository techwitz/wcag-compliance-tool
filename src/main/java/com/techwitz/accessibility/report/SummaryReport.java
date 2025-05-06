package com.techwitz.accessibility.report;

import java.util.HashMap;
import java.util.Map;

public class SummaryReport {
    private final int totalPages;
    private final int totalViolations;
    private final int totalFixesApplied;
    private final Map<String, Integer> violationsByPage;

    // Constructor and builder pattern implementation
    private SummaryReport(Builder builder) {
        this.totalPages = builder.totalPages;
        this.totalViolations = builder.totalViolations;
        this.totalFixesApplied = builder.totalFixesApplied;
        this.violationsByPage = builder.violationsByPage;
    }

    // Getter methods
    public int getTotalPages() { return totalPages; }
    public int getTotalViolations() { return totalViolations; }
    public int getTotalFixesApplied() { return totalFixesApplied; }
    public Map<String, Integer> getViolationsByPage() { return violationsByPage; }

    /**
     * Builder for creating SummaryReport instances
     */
    public static class Builder {
        private int totalPages;
        private int totalViolations;
        private int totalFixesApplied;
        private Map<String, Integer> violationsByPage = new HashMap<>();

        public Builder setTotalPages(int totalPages) {
            this.totalPages = totalPages;
            return this;
        }

        public Builder setTotalViolations(int totalViolations) {
            this.totalViolations = totalViolations;
            return this;
        }

        public Builder setTotalFixesApplied(int totalFixesApplied) {
            this.totalFixesApplied = totalFixesApplied;
            return this;
        }

        public Builder setViolationsByPage(Map<String, Integer> violationsByPage) {
            this.violationsByPage = new HashMap<>(violationsByPage);
            return this;
        }

        public SummaryReport build() {
            return new SummaryReport(this);
        }
    }
}
