package com.techwitz.accessibility.report;

import com.techwitz.accessibility.Violation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Report of compliance analysis for a single page
 */
public class ComplianceReport {
    private final String pageUrl;
    private final int totalViolations;
    private final int autoFixableViolations;
    private final Map<String, Integer> violationsByLevel;
    private final List<Violation> violations;

    private ComplianceReport(Builder builder) {
        this.pageUrl = builder.pageUrl;
        this.totalViolations = builder.totalViolations;
        this.autoFixableViolations = builder.autoFixableViolations;
        this.violationsByLevel = builder.violationsByLevel;
        this.violations = builder.violations;
    }

    public String getPageUrl() {
        return pageUrl;
    }

    public int getTotalViolations() {
        return totalViolations;
    }

    public int getAutoFixableViolations() {
        return autoFixableViolations;
    }

    public Map<String, Integer> getViolationsByLevel() {
        return violationsByLevel;
    }

    public List<Violation> getViolations() {
        return violations;
    }

    /**
     * Builder for creating ComplianceReport instances
     */
    public static class Builder {
        private String pageUrl;
        private int totalViolations;
        private int autoFixableViolations;
        private Map<String, Integer> violationsByLevel = new HashMap<>();
        private List<Violation> violations = new ArrayList<>();

        public Builder setPageUrl(String pageUrl) {
            this.pageUrl = pageUrl;
            return this;
        }

        public Builder setTotalViolations(int totalViolations) {
            this.totalViolations = totalViolations;
            return this;
        }

        public Builder setAutoFixableViolations(int autoFixableViolations) {
            this.autoFixableViolations = autoFixableViolations;
            return this;
        }

        public Builder setViolationsByLevel(Map<String, Integer> violationsByLevel) {
            this.violationsByLevel = new HashMap<>(violationsByLevel);
            return this;
        }

        public Builder setViolations(List<Violation> violations) {
            this.violations = new ArrayList<>(violations);
            return this;
        }

        public ComplianceReport build() {
            return new ComplianceReport(this);
        }
    }
}

