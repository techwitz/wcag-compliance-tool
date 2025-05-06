package com.techwitz.accessibility;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of a remediation operation
 */
public class RemediationResult {
    private final String pageUrl;
    private final String originalHtml;
    private final String remediatedHtml;
    private final List<Violation> violations;
    private final int fixesApplied;
    private final List<String> changeLog;
    private final String errorMessage;

    private RemediationResult(Builder builder) {
        this.pageUrl = builder.pageUrl;
        this.originalHtml = builder.originalHtml;
        this.remediatedHtml = builder.remediatedHtml;
        this.violations = builder.violations;
        this.fixesApplied = builder.fixesApplied;
        this.changeLog = builder.changeLog;
        this.errorMessage = builder.errorMessage;
    }

    public String getPageUrl() {
        return pageUrl;
    }

    public String getOriginalHtml() {
        return originalHtml;
    }

    public String getRemediatedHtml() {
        return remediatedHtml;
    }

    public List<Violation> getViolations() {
        return violations;
    }

    public int getFixesApplied() {
        return fixesApplied;
    }

    public List<String> getChangeLog() {
        return changeLog;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public boolean hasError() {
        return errorMessage != null && !errorMessage.isEmpty();
    }

    /**
     * Builder for creating RemediationResult instances
     */
    public static class Builder {
        private String pageUrl;
        private String originalHtml;
        private String remediatedHtml;
        private List<Violation> violations = new ArrayList<>();
        private int fixesApplied = 0;
        private List<String> changeLog = new ArrayList<>();
        private String errorMessage;

        public Builder setPageUrl(String pageUrl) {
            this.pageUrl = pageUrl;
            return this;
        }

        public Builder setOriginalHtml(String originalHtml) {
            this.originalHtml = originalHtml;
            return this;
        }

        public Builder setRemediatedHtml(String remediatedHtml) {
            this.remediatedHtml = remediatedHtml;
            return this;
        }

        public Builder setViolations(List<Violation> violations) {
            this.violations = new ArrayList<>(violations);
            return this;
        }

        public Builder setFixesApplied(int fixesApplied) {
            this.fixesApplied = fixesApplied;
            return this;
        }

        public Builder setChangeLog(List<String> changeLog) {
            this.changeLog = new ArrayList<>(changeLog);
            return this;
        }

        public Builder setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public RemediationResult build() {
            return new RemediationResult(this);
        }
    }
}
