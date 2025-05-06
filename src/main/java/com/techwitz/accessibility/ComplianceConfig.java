package com.techwitz.accessibility;

import com.techwitz.accessibility.rule.WcagRule;

import java.util.*;

/**
 * Configuration options for the compliance tool
 */
public class ComplianceConfig {
    private final List<WcagRule> customRules;
    private final boolean includeLevel2Rules;
    private final boolean includeLevel3Rules;
    private final Map<String, String> cssProperties;
    private final Set<String> excludedPages;

    private ComplianceConfig(Builder builder) {
        this.customRules = builder.customRules;
        this.includeLevel2Rules = builder.includeLevel2Rules;
        this.includeLevel3Rules = builder.includeLevel3Rules;
        this.cssProperties = builder.cssProperties;
        this.excludedPages = builder.excludedPages;
    }

    public List<WcagRule> getCustomRules() {
        return customRules;
    }

    public boolean includeLevel2Rules() {
        return includeLevel2Rules;
    }

    public boolean includeLevel3Rules() {
        return includeLevel3Rules;
    }

    public Map<String, String> getCssProperties() {
        return cssProperties;
    }

    public Set<String> getExcludedPages() {
        return excludedPages;
    }

    /**
     * Builder for creating ComplianceConfig instances
     */
    public static class Builder {
        private List<WcagRule> customRules = new ArrayList<>();
        private boolean includeLevel2Rules = true;
        private boolean includeLevel3Rules = false;
        private Map<String, String> cssProperties = new HashMap<>();
        private Set<String> excludedPages = new HashSet<>();

        public Builder addCustomRule(WcagRule rule) {
            customRules.add(rule);
            return this;
        }

        public Builder setIncludeLevel2Rules(boolean include) {
            includeLevel2Rules = include;
            return this;
        }

        public Builder setIncludeLevel3Rules(boolean include) {
            includeLevel3Rules = include;
            return this;
        }

        public Builder setCssProperty(String property, String value) {
            cssProperties.put(property, value);
            return this;
        }

        public Builder addExcludedPage(String pagePattern) {
            excludedPages.add(pagePattern);
            return this;
        }

        public ComplianceConfig build() {
            return new ComplianceConfig(this);
        }
    }
}
