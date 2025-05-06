package com.techwitz.accessibility;

/**
 * Represents a single violation of a WCAG rule
 */
public class Violation {
    private final String ruleId;
    private final String message;
    private final String element;
    private final String xpath;
    private final String severity;
    private final boolean autoFixable;
    private final String remediation;

    private Violation(Builder builder) {
        this.ruleId = builder.ruleId;
        this.message = builder.message;
        this.element = builder.element;
        this.xpath = builder.xpath;
        this.severity = builder.severity;
        this.autoFixable = builder.autoFixable;
        this.remediation = builder.remediation;
    }

    public String getRuleId() {
        return ruleId;
    }

    public String getMessage() {
        return message;
    }

    public String getElement() {
        return element;
    }

    public String getXpath() {
        return xpath;
    }

    public String getSeverity() {
        return severity;
    }

    public boolean isAutoFixable() {
        return autoFixable;
    }

    public String getRemediation() {
        return remediation;
    }

    public static class Builder {
        private String ruleId;
        private String message;
        private String element;
        private String xpath;
        private String severity = "critical";
        private boolean autoFixable = false;
        private String remediation;

        public Builder setRuleId(String ruleId) {
            this.ruleId = ruleId;
            return this;
        }

        public Builder setMessage(String message) {
            this.message = message;
            return this;
        }

        public Builder setElement(String element) {
            this.element = element;
            return this;
        }

        public Builder setXpath(String xpath) {
            this.xpath = xpath;
            return this;
        }

        public Builder setSeverity(String severity) {
            this.severity = severity;
            return this;
        }

        public Builder setAutoFixable(boolean autoFixable) {
            this.autoFixable = autoFixable;
            return this;
        }

        public Builder setRemediation(String remediation) {
            this.remediation = remediation;
            return this;
        }

        public Violation build() {
            return new Violation(this);
        }
    }
}
