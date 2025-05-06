package com.techwitz.accessibility;

import com.techwitz.accessibility.rule.WcagRule;
import com.techwitz.accessibility.rule.WcagRuleEngine;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Service for applying remediation to WCAG violations
 */
public class RemediationService {
    private static final Logger LOGGER = Logger.getLogger(RemediationService.class.getName());

    private final ComplianceConfig config;

    public RemediationService(ComplianceConfig config) {
        this.config = config;
    }

    /**
     * Applies fixes to violations found in a document
     * @param document Document to fix
     * @param violations List of violations to fix
     * @param options Remediation options
     * @return Result containing fixed HTML and change log
     */
    public RemediationResult applyFixes(Document document,
                                        List<Violation> violations,
                                        RemediationOptions options) {
        if (!options.isAutoFix()) {
            return new RemediationResult.Builder()
                    .setPageUrl(document.baseUri())
                    .setOriginalHtml(document.outerHtml())
                    .setRemediatedHtml(document.outerHtml())
                    .setViolations(violations)
                    .setFixesApplied(0)
                    .build();
        }

        Document workingCopy = document.clone();
        List<String> changeLog = new ArrayList<>();
        int fixesApplied = 0;

        // Group violations by rule ID
        Map<String, List<Violation>> violationsByRule = violations.stream()
                .collect(Collectors.groupingBy(Violation::getRuleId));

        // Get all rules from violation IDs
        Map<String, WcagRule> ruleMap = new HashMap<>();
        for (WcagRule rule : new WcagRuleEngine(config).getRules()) {
            ruleMap.put(rule.getId(), rule);
        }

        // Apply fixes for each rule
        for (Map.Entry<String, List<Violation>> entry : violationsByRule.entrySet()) {
            String ruleId = entry.getKey();
            List<Violation> ruleViolations = entry.getValue();

            WcagRule rule = ruleMap.get(ruleId);
            if (rule != null && rule.canAutoFix()) {
                try {
                    int fixes = rule.applyFixes(workingCopy, ruleViolations);
                    fixesApplied += fixes;

                    if (fixes > 0) {
                        changeLog.add("Applied " + fixes + " fixes for rule: " + rule.getDescription());
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error applying fixes for rule " + ruleId, e);
                    changeLog.add("Error fixing " + ruleId + ": " + e.getMessage());
                }
            }
        }

        // Apply ARIA enhancements if requested
        if (options.isApplyAriaEnhancements()) {
            int ariaEnhancements = applyAriaEnhancements(workingCopy);
            fixesApplied += ariaEnhancements;
            if (ariaEnhancements > 0) {
                changeLog.add("Applied " + ariaEnhancements + " ARIA enhancements");
            }
        }

        // Fix contrast issues if requested
        if (options.isFixContrastIssues()) {
            int contrastFixes = fixContrastIssues(workingCopy);
            fixesApplied += contrastFixes;
            if (contrastFixes > 0) {
                changeLog.add("Applied " + contrastFixes + " contrast fixes");
            }
        }

        return new RemediationResult.Builder()
                .setPageUrl(document.baseUri())
                .setOriginalHtml(document.outerHtml())
                .setRemediatedHtml(workingCopy.outerHtml())
                .setViolations(violations)
                .setFixesApplied(fixesApplied)
                .setChangeLog(changeLog)
                .build();
    }

    /**
     * Applies ARIA attributes to improve accessibility
     * @param document Document to enhance
     * @return Number of enhancements applied
     */
    private int applyAriaEnhancements(Document document) {
        int enhancements = 0;

        // Add landmarks to major sections
        Elements mainElements = document.select("div[id=main], div[id=content], div.main, div.content");
        for (Element element : mainElements) {
            if (!element.hasAttr("role")) {
                element.attr("role", "main");
                enhancements++;
            }
        }

        // Add navigation landmarks
        Elements navElements = document.select("nav, div[id=nav], div[id=navigation], div.nav, div.navigation");
        for (Element element : navElements) {
            if (!element.hasAttr("role")) {
                element.attr("role", "navigation");
                enhancements++;
            }
        }

        // Add appropriate roles to common components
        Elements searchForms = document.select("form[action*=search], form[id*=search]");
        for (Element element : searchForms) {
            if (!element.hasAttr("role")) {
                element.attr("role", "search");
                enhancements++;
            }
        }

        // Add aria-required to required form fields
        Elements requiredFields = document.select("input[required], select[required], textarea[required]");
        for (Element element : requiredFields) {
            if (!element.hasAttr("aria-required")) {
                element.attr("aria-required", "true");
                enhancements++;
            }
        }

        // Add aria-expanded to dropdown toggles
        Elements dropdownToggles = document.select("[data-toggle=dropdown], .dropdown-toggle");
        for (Element element : dropdownToggles) {
            if (!element.hasAttr("aria-expanded")) {
                element.attr("aria-expanded", "false");
                enhancements++;
            }
        }

        return enhancements;
    }

    /**
     * Fixes contrast issues in the document
     * @param document Document to fix
     * @return Number of fixes applied
     */
    private int fixContrastIssues(Document document) {
        int fixes = 0;

        // Add a base style with minimum contrast for all text
        Element head = document.head();
        if (head != null) {
            Element style = head.appendElement("style");
            style.attr("type", "text/css");
            style.text(
                    "/* WCAG 2.3 Contrast Enhancements */\n" +
                            "body { color: #333; background-color: #fff; }\n" +
                            "a { color: #0056b3; }\n" +
                            "a:visited { color: #551A8B; }\n" +
                            "a:hover, a:focus { color: #003d7a; text-decoration: underline; }\n" +
                            "button, .btn { color: #fff; background-color: #0056b3; }\n" +
                            ".btn-secondary { color: #fff; background-color: #6c757d; }\n" +
                            ".btn-danger { color: #fff; background-color: #dc3545; }\n" +
                            ".btn-success { color: #fff; background-color: #28a745; }\n"
            );
            fixes++;
        }

        // Fix specific contrast issues from config CSS properties
        Map<String, String> cssProperties = config.getCssProperties();
        if (!cssProperties.isEmpty() && head != null) {
            Element style = head.appendElement("style");
            style.attr("type", "text/css");
            StringBuilder css = new StringBuilder("/* Custom Contrast Fixes */\n");

            for (Map.Entry<String, String> entry : cssProperties.entrySet()) {
                css.append(entry.getKey()).append(" { ").append(entry.getValue()).append(" }\n");
                fixes++;
            }

            style.text(css.toString());
        }

        return fixes;
    }
}