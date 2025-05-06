package com.techwitz.accessibility;

import com.techwitz.accessibility.report.ComplianceReport;
import com.techwitz.accessibility.rule.WcagRule;
import com.techwitz.accessibility.rule.WcagRuleEngine;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Core tool for analyzing and remediating WCAG 2.3 accessibility issues
 */
public class WcagComplianceTool {
    private static final Logger LOGGER = Logger.getLogger(WcagComplianceTool.class.getName());

    private final WcagRuleEngine ruleEngine;
    private final RemediationService remediationService;
    private final ComplianceConfig config;

    /**
     * Constructor for the compliance tool
     * @param config Configuration options for the tool
     */
    public WcagComplianceTool(ComplianceConfig config) {
        this.config = config;
        this.ruleEngine = new WcagRuleEngine(config);
        this.remediationService = new RemediationService(config);
    }

    /**
     * Get the rule engine
     * @return The rule engine instance
     */
    public WcagRuleEngine getRuleEngine() {
        return ruleEngine;
    }

    /**
     * Get the remediation service
     * @return The remediation service instance
     */
    public RemediationService getRemediationService() {
        return remediationService;
    }

    /**
     * Analyzes a page and identifies WCAG 2.3 violations
     * @param pageUrl URL or file path to analyze
     * @return ComplianceReport containing violations and recommendations
     */
    public ComplianceReport analyzePage(String pageUrl) throws IOException {
        Document document = parsePage(pageUrl);
        List<Violation> violations = ruleEngine.evaluateRules(document);
        return generateReport(pageUrl, violations);
    }

    /**
     * Applies automatic fixes to accessibility issues
     * @param pageUrl URL or file path to remediate
     * @param options Options for controlling remediation behavior
     * @return RemediationResult containing modified HTML and log of changes
     */
    public RemediationResult remediatePage(String pageUrl, RemediationOptions options) throws IOException {
        Document document = parsePage(pageUrl);
        List<Violation> violations = ruleEngine.evaluateRules(document);
        RemediationResult result = remediationService.applyFixes(document, violations, options);

        // Save the remediated file if path is provided
        if (options.getSaveToPath() != null) {
            String outputPath = options.getSaveToPath();
            Files.writeString(Paths.get(outputPath), result.getRemediatedHtml());
            LOGGER.info("Remediated HTML saved to: " + outputPath);
        }

        return result;
    }

    /**
     * Batch process multiple pages
     * @param pageUrls Collection of URLs or file paths to process
     * @param options Remediation options
     * @return Map of URL to remediation results
     */
    public Map<String, RemediationResult> batchProcess(Collection<String> pageUrls,
                                                       RemediationOptions options) {
        Map<String, RemediationResult> results = new HashMap<>();

        for (String pageUrl : pageUrls) {
            try {
                RemediationResult result = remediatePage(pageUrl, options);
                results.put(pageUrl, result);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error processing " + pageUrl, e);
                RemediationResult errorResult = new RemediationResult.Builder()
                        .setPageUrl(pageUrl)
                        .setErrorMessage(e.getMessage())
                        .build();
                results.put(pageUrl, errorResult);
            }
        }

        return results;
    }

    /**
     * Parse HTML from a file or URL
     * @param source File path or URL to parse
     * @return Parsed Document object
     */
    private Document parsePage(String source) throws IOException {
        if (source.startsWith("http://") || source.startsWith("https://")) {
            LOGGER.info("Parsing URL: " + source);
            return Jsoup.connect(source)
                    .userAgent("WCAG Compliance Tool/1.0")
                    .timeout(10000)
                    .get();
        } else {
            LOGGER.info("Parsing file: " + source);
            File input = new File(source);
            return Jsoup.parse(input, "UTF-8");
        }
    }

    /**
     * Generate a compliance report for results
     * @param pageUrl URL or file path that was analyzed
     * @param violations List of violations found
     * @return ComplianceReport containing analysis results
     */
    private ComplianceReport generateReport(String pageUrl, List<Violation> violations) {
        int totalViolations = violations.size();
        int autoFixableViolations = (int) violations.stream()
                .filter(Violation::isAutoFixable)
                .count();

        Map<String, Integer> violationsByLevel = new HashMap<>();
        violationsByLevel.put("A", 0);
        violationsByLevel.put("AA", 0);
        violationsByLevel.put("AAA", 0);

        // Count violations by WCAG level
        for (Violation violation : violations) {
            String ruleId = violation.getRuleId();

            // Find the rule that generated this violation
            Optional<WcagRule> rule = ruleEngine.getRules().stream()
                    .filter(r -> r.getId().equals(ruleId))
                    .findFirst();

            if (rule.isPresent()) {
                String level = rule.get().getLevel();
                violationsByLevel.put(level, violationsByLevel.getOrDefault(level, 0) + 1);
            }
        }

        return new ComplianceReport.Builder()
                .setPageUrl(pageUrl)
                .setTotalViolations(totalViolations)
                .setAutoFixableViolations(autoFixableViolations)
                .setViolationsByLevel(violationsByLevel)
                .setViolations(violations)
                .build();
    }
}
