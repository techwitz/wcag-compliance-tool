package com.techwitz.accessibility;

/**
 * Options for controlling remediation behavior
 */
public class RemediationOptions {
    private final boolean autoFix;
    private final boolean preserveComments;
    private final String saveToPath;
    private final boolean applyAriaEnhancements;
    private final boolean fixContrastIssues;

    private RemediationOptions(Builder builder) {
        this.autoFix = builder.autoFix;
        this.preserveComments = builder.preserveComments;
        this.saveToPath = builder.saveToPath;
        this.applyAriaEnhancements = builder.applyAriaEnhancements;
        this.fixContrastIssues = builder.fixContrastIssues;
    }

    public boolean isAutoFix() {
        return autoFix;
    }

    public boolean isPreserveComments() {
        return preserveComments;
    }

    public String getSaveToPath() {
        return saveToPath;
    }

    public boolean isApplyAriaEnhancements() {
        return applyAriaEnhancements;
    }

    public boolean isFixContrastIssues() {
        return fixContrastIssues;
    }

    /**
     * Builder for creating RemediationOptions instances
     */
    public static class Builder {
        private boolean autoFix = true;
        private boolean preserveComments = true;
        private String saveToPath = null;
        private boolean applyAriaEnhancements = true;
        private boolean fixContrastIssues = true;

        public Builder() {
            // Default constructor
        }

        /**
         * Copy constructor
         * @param options Options to copy from
         */
        public Builder(RemediationOptions options) {
            this.autoFix = options.isAutoFix();
            this.preserveComments = options.isPreserveComments();
            this.saveToPath = options.getSaveToPath();
            this.applyAriaEnhancements = options.isApplyAriaEnhancements();
            this.fixContrastIssues = options.isFixContrastIssues();
        }

        public Builder setAutoFix(boolean autoFix) {
            this.autoFix = autoFix;
            return this;
        }

        public Builder setPreserveComments(boolean preserveComments) {
            this.preserveComments = preserveComments;
            return this;
        }

        public Builder setSaveToPath(String saveToPath) {
            this.saveToPath = saveToPath;
            return this;
        }

        public Builder setApplyAriaEnhancements(boolean applyAriaEnhancements) {
            this.applyAriaEnhancements = applyAriaEnhancements;
            return this;
        }

        public Builder setFixContrastIssues(boolean fixContrastIssues) {
            this.fixContrastIssues = fixContrastIssues;
            return this;
        }

        public RemediationOptions build() {
            return new RemediationOptions(this);
        }
    }
}
