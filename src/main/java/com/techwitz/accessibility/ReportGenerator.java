package com.techwitz.accessibility;

import com.techwitz.accessibility.report.ComplianceReport;
import com.techwitz.accessibility.report.SummaryReport;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Generator for compliance reports
 */
public class ReportGenerator {
    private final ComplianceConfig config;

    public ReportGenerator(ComplianceConfig config) {
        this.config = config;
    }

    /**
     * Generates a compliance report for a single page
     * @param pageUrl URL of the page analyzed
     * @param violations List of violations found
     * @return Compliance report
     */
    public ComplianceReport generateReport(String pageUrl, List<Violation> violations) {
        int totalViolations = violations.size();
        int autoFixableViolations = (int) violations.stream()
                .filter(Violation::isAutoFixable)
                .count();

        Map<String, Integer> violationsByLevel = new HashMap<>();
        violationsByLevel.put("A", 0);
        violationsByLevel.put("AA", 0);
        violationsByLevel.put("AAA", 0);

        // TODO: Calculate violations by level

        return new ComplianceReport.Builder()
                .setPageUrl(pageUrl)
                .setTotalViolations(totalViolations)
                .setAutoFixableViolations(autoFixableViolations)
                .setViolationsByLevel(violationsByLevel)
                .setViolations(violations)
                .build();
    }

    /**
     * Generates a summary report for multiple pages
     * @param results Map of URL to remediation results
     * @return Summary report
     */
    public SummaryReport generateSummaryReport(Map<String, RemediationResult> results) {
        int totalPages = results.size();
        int totalViolations = 0;
        int totalFixesApplied = 0;

        Map<String, Integer> violationsByPage = new HashMap<>();

        for (Map.Entry<String, RemediationResult> entry : results.entrySet()) {
            String pageUrl = entry.getKey();
            RemediationResult result = entry.getValue();

            int pageViolations = result.getViolations().size();
            totalViolations += pageViolations;
            totalFixesApplied += result.getFixesApplied();

            violationsByPage.put(pageUrl, pageViolations);
        }

        return new SummaryReport.Builder()
                .setTotalPages(totalPages)
                .setTotalViolations(totalViolations)
                .setTotalFixesApplied(totalFixesApplied)
                .setViolationsByPage(violationsByPage)
                .build();
    }
}

