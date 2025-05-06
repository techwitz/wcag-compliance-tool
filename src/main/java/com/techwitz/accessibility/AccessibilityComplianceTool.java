package com.techwitz.accessibility;

import com.techwitz.accessibility.report.ComplianceReport;
import com.techwitz.accessibility.report.SummaryReport;
import com.techwitz.accessibility.rule.WcagRuleEngine;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main entry point for the WCAG 2.3 Compliance Tool
 */
public class AccessibilityComplianceTool {
    private static final Logger LOGGER = Logger.getLogger(AccessibilityComplianceTool.class.getName());

    private final HtmlParser parser;
    private final WcagRuleEngine ruleEngine;
    private final RemediationService remediationService;
    private final ReportGenerator reportGenerator;
    private final ComplianceConfig config;

    /**
     * Constructor for the compliance tool
     * @param config Configuration options for the tool
     */
    public AccessibilityComplianceTool(ComplianceConfig config) {
        this.config = config;
        this.parser = new HtmlParser();
        this.ruleEngine = new WcagRuleEngine(config);
        this.remediationService = new RemediationService(config);
        this.reportGenerator = new ReportGenerator(config);
    }

    /**
     * Analyzes a page and identifies WCAG 2.3 violations
     * @param pageUrl URL or file path to analyze
     * @return ComplianceReport containing violations and recommendations
     */
    public ComplianceReport analyzePage(String pageUrl) throws IOException {
        Document document = parser.parse(pageUrl);
        List<Violation> violations = ruleEngine.evaluateRules(document);
        return reportGenerator.generateReport(pageUrl, violations);
    }

    /**
     * Applies automatic fixes to accessibility issues
     * @param pageUrl URL or file path to remediate
     * @param options Options for controlling remediation behavior
     * @return RemediationResult containing modified HTML and log of changes
     */
    public RemediationResult remediatePage(String pageUrl, RemediationOptions options) throws IOException {
        Document document = parser.parse(pageUrl);
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
        ExecutorService executor = Executors.newFixedThreadPool(
                Math.min(Runtime.getRuntime().availableProcessors(), pageUrls.size()));

        Map<String, RemediationResult> results = new ConcurrentHashMap<>();

        for (String pageUrl : pageUrls) {
            executor.submit(() -> {
                try {
                    RemediationResult result = remediatePage(pageUrl, options);
                    results.put(pageUrl, result);
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error processing " + pageUrl, e);
                    results.put(pageUrl, new RemediationResult.Builder()
                            .setPageUrl(pageUrl)
                            .setErrorMessage(e.getMessage())
                            .build());
                }
            });
        }

        executor.shutdown();
        try {
            executor.awaitTermination(1, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.log(Level.SEVERE, "Batch processing interrupted", e);
        }

        return results;
    }

    /**
     * Generates a summary report for multiple pages
     * @param results Map of URL to remediation results
     * @return SummaryReport for all processed pages
     */
    public SummaryReport generateSummaryReport(Map<String, RemediationResult> results) {
        return reportGenerator.generateSummaryReport(results);
    }
}
