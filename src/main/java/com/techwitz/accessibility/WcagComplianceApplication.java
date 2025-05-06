package com.techwitz.accessibility;

import com.techwitz.accessibility.report.SummaryReport;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;

public class WcagComplianceApplication {
    private static final Logger LOGGER = Logger.getLogger(WcagComplianceApplication.class.getName());
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(WcagComplianceApplication.class);

    public static void main(String[] args) {
        try {
            // Parse command line arguments
            Map<String, String> arguments = parseArguments(args);
            String applicationPath = arguments.getOrDefault("--app-path", ".");
            String outputDir = arguments.getOrDefault("--output-dir", "wcag-reports");

            log.info("Application path: {} and output: {}", applicationPath, outputDir);

            boolean autoFix = Boolean.parseBoolean(arguments.getOrDefault("--auto-fix", "true"));
            boolean includeLevel2 = Boolean.parseBoolean(arguments.getOrDefault("--include-aa", "true"));
            boolean includeLevel3 = Boolean.parseBoolean(arguments.getOrDefault("--include-aaa", "false"));

            // Create output directory if it doesn't exist
            Files.createDirectories(Paths.get(outputDir));

            // Configure the tool
            ComplianceConfig config = createComplianceConfig(includeLevel2, includeLevel3);
            AccessibilityComplianceTool tool = new AccessibilityComplianceTool(config);

            // Find all HTML files in the application path
            List<String> htmlFiles = findHtmlFiles(applicationPath);
            LOGGER.info("Found " + htmlFiles.size() + " HTML files to analyze");

            if (htmlFiles.isEmpty()) {
                System.out.println("No HTML files found in path: " + applicationPath);
                return;
            }

            // Create remediation options
            RemediationOptions options = new RemediationOptions.Builder()
                    .setAutoFix(autoFix)
                    .setApplyAriaEnhancements(true)
                    .setFixContrastIssues(true)
                    .build();

            // Process all HTML files
            Map<String, RemediationResult> results = processHtmlFiles(tool, htmlFiles, options, outputDir, autoFix);

            // Generate summary report
            SummaryReport summary = tool.generateSummaryReport(results);
            generateHtmlSummaryReport(summary, results, outputDir);

            // Print summary
            printSummary(summary);

        } catch (Exception e) {
            LOGGER.severe("Error running WCAG compliance check: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Parse command line arguments
     */
    private static Map<String, String> parseArguments(String[] args) {
        Map<String, String> arguments = new HashMap<>();

        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("--") && i < args.length - 1) {
                arguments.put(args[i], args[i + 1]);
                i++;
            }
        }

        return arguments;
    }

    /**
     * Create configuration for the compliance tool
     */
    private static ComplianceConfig createComplianceConfig(boolean includeLevel2, boolean includeLevel3) {
        ComplianceConfig.Builder builder = new ComplianceConfig.Builder()
                .setIncludeLevel2Rules(includeLevel2)
                .setIncludeLevel3Rules(includeLevel3);

        // Add common CSS properties to fix contrast issues
        builder.setCssProperty(".text-light", "color: #000000;")
                .setCssProperty(".btn-light", "color: #000000; background-color: #f8f9fa;")
                .setCssProperty(".navbar-light", "color: #000000;")
                .setCssProperty(".text-white", "color: #000000;")
                .setCssProperty("a", "color: #0056b3;");

        // Add pages to exclude (e.g., third-party content)
        builder.addExcludedPage("/vendor/")
                .addExcludedPage("/node_modules/")
                .addExcludedPage("/dist/");

        return builder.build();
    }

    /**
     * Find all HTML files in the application path
     */
    private static List<String> findHtmlFiles(String applicationPath) throws IOException {
        List<String> htmlFiles = new ArrayList<>();

        Files.walk(Paths.get(applicationPath))
                .filter(Files::isRegularFile)
                .filter(path -> {
                    String fileName = path.toString().toLowerCase();
                    return fileName.endsWith(".html") || fileName.endsWith(".htm");
                })
                .forEach(path -> htmlFiles.add(path.toString()));

        return htmlFiles;
    }

    /**
     * Process all HTML files and generate reports
     */
    private static Map<String, RemediationResult> processHtmlFiles(
            AccessibilityComplianceTool tool,
            List<String> htmlFiles,
            RemediationOptions options,
            String outputDir,
            boolean autoFix) throws IOException {

        Map<String, RemediationResult> results = new HashMap<>();

        int count = 0;
        for (String filePath : htmlFiles) {
            try {
                count++;
                System.out.println("Processing file " + count + "/" + htmlFiles.size() + ": " + filePath);

                // If auto-fix is enabled, save the remediated files
                RemediationOptions fileOptions = options;
                if (autoFix) {
                    String fileName = new File(filePath).getName();
                    String outputPath = Paths.get(outputDir, "fixed_" + fileName).toString();
                    fileOptions = new RemediationOptions.Builder()
                            .setAutoFix(options.isAutoFix())
                            .setApplyAriaEnhancements(options.isApplyAriaEnhancements())
                            .setFixContrastIssues(options.isFixContrastIssues())
                            .setSaveToPath(outputPath)
                            .build();
                }

                // Process the file
                RemediationResult result = tool.remediatePage(filePath, fileOptions);
                results.put(filePath, result);

                // Generate individual HTML report
                generateHtmlReport(result, outputDir);

            } catch (Exception e) {
                LOGGER.warning("Error processing file " + filePath + ": " + e.getMessage());
            }
        }

        return results;
    }

    /**
     * Generate HTML report for a single file
     */
    private static void generateHtmlReport(RemediationResult result, String outputDir) throws IOException {
        String fileName = new File(result.getPageUrl()).getName();
        String reportPath = Paths.get(outputDir, "report_" + fileName).toString();

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"en\">\n");
        html.append("<head>\n");
        html.append("    <meta charset=\"UTF-8\">\n");
        html.append("    <title>WCAG Compliance Report: ").append(fileName).append("</title>\n");
        html.append("    <style>\n");
        html.append("        body { font-family: Arial, sans-serif; line-height: 1.6; max-width: 1200px; margin: 0 auto; padding: 20px; }\n");
        html.append("        h1, h2 { color: #333; }\n");
        html.append("        .summary { background: #f5f5f5; padding: 15px; border-radius: 5px; margin-bottom: 20px; }\n");
        html.append("        .violation { background: #fff8f8; border-left: 4px solid #ff6b6b; padding: 15px; margin-bottom: 10px; }\n");
        html.append("        .fixed { background: #f0fff0; border-left: 4px solid #4caf50; }\n");
        html.append("        .violation h3 { margin-top: 0; color: #d32f2f; }\n");
        html.append("        .fixed h3 { color: #388e3c; }\n");
        html.append("        pre { background: #f8f8f8; padding: 10px; overflow: auto; }\n");
        html.append("        .critical { color: #d32f2f; }\n");
        html.append("        .serious { color: #f57c00; }\n");
        html.append("        .moderate { color: #fbc02d; }\n");
        html.append("        .minor { color: #7cb342; }\n");
        html.append("    </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");

        // Header
        html.append("    <h1>WCAG 2.3 Compliance Report</h1>\n");
        html.append("    <p>File: ").append(result.getPageUrl()).append("</p>\n");

        // Summary
        html.append("    <div class=\"summary\">\n");
        html.append("        <h2>Summary</h2>\n");
        html.append("        <p>Total violations: ").append(result.getViolations().size()).append("</p>\n");
        html.append("        <p>Fixes applied: ").append(result.getFixesApplied()).append("</p>\n");
        html.append("    </div>\n");

        // Violations
        html.append("    <h2>Violations</h2>\n");

        for (Violation violation : result.getViolations()) {
            String violationClass = "violation";
            if (violation.isAutoFixable() && result.getFixesApplied() > 0) {
                violationClass += " fixed";
            }

            html.append("    <div class=\"").append(violationClass).append("\">\n");
            html.append("        <h3>").append(violation.getMessage()).append("</h3>\n");
            html.append("        <p>Rule: ").append(violation.getRuleId()).append("</p>\n");
            html.append("        <p>Severity: <span class=\"").append(violation.getSeverity().toLowerCase()).append("\">")
                    .append(violation.getSeverity()).append("</span></p>\n");
            html.append("        <p>Auto-fixable: ").append(violation.isAutoFixable()).append("</p>\n");
            html.append("        <p>Remediation: ").append(violation.getRemediation()).append("</p>\n");
            html.append("        <p>Element:</p>\n");
            html.append("        <pre>").append(escapeHtml(violation.getElement())).append("</pre>\n");
            html.append("    </div>\n");
        }

        // Change log
        if (!result.getChangeLog().isEmpty()) {
            html.append("    <h2>Changes Applied</h2>\n");
            html.append("    <ul>\n");
            for (String change : result.getChangeLog()) {
                html.append("        <li>").append(change).append("</li>\n");
            }
            html.append("    </ul>\n");
        }

        html.append("</body>\n");
        html.append("</html>");

        Files.writeString(Paths.get(reportPath), html.toString());
        LOGGER.info("Generated report: " + reportPath);
    }

    /**
     * Generate HTML summary report
     */
    private static void generateHtmlSummaryReport(
            SummaryReport summary,
            Map<String, RemediationResult> results,
            String outputDir) throws IOException {

        String reportPath = Paths.get(outputDir, "summary_report.html").toString();

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"en\">\n");
        html.append("<head>\n");
        html.append("    <meta charset=\"UTF-8\">\n");
        html.append("    <title>WCAG Compliance Summary Report</title>\n");
        html.append("    <style>\n");
        html.append("        body { font-family: Arial, sans-serif; line-height: 1.6; max-width: 1200px; margin: 0 auto; padding: 20px; }\n");
        html.append("        h1, h2 { color: #333; }\n");
        html.append("        .summary { background: #f5f5f5; padding: 15px; border-radius: 5px; margin-bottom: 20px; }\n");
        html.append("        table { width: 100%; border-collapse: collapse; margin: 20px 0; }\n");
        html.append("        th, td { border: 1px solid #ddd; padding: 10px; text-align: left; }\n");
        html.append("        th { background-color: #f2f2f2; }\n");
        html.append("        tr:nth-child(even) { background-color: #f9f9f9; }\n");
        html.append("        .progress-bar { background: #e0e0e0; height: 20px; border-radius: 10px; overflow: hidden; }\n");
        html.append("        .progress-fill { background: #4caf50; height: 100%; }\n");
        html.append("    </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");

        // Header
        html.append("    <h1>WCAG 2.3 Compliance Summary Report</h1>\n");
        html.append("    <p>Generated on: ").append(new Date()).append("</p>\n");

        // Summary
        html.append("    <div class=\"summary\">\n");
        html.append("        <h2>Summary</h2>\n");
        html.append("        <p>Total pages analyzed: ").append(summary.getTotalPages()).append("</p>\n");
        html.append("        <p>Total violations found: ").append(summary.getTotalViolations()).append("</p>\n");
        html.append("        <p>Total fixes applied: ").append(summary.getTotalFixesApplied()).append("</p>\n");

        // Compliance percentage
        if (summary.getTotalViolations() > 0) {
            double compliancePercentage = (double) summary.getTotalFixesApplied() / summary.getTotalViolations() * 100;
            html.append("        <p>Automated compliance improvement: ").append(String.format("%.1f", compliancePercentage)).append("%</p>\n");
            html.append("        <div class=\"progress-bar\">\n");
            html.append("            <div class=\"progress-fill\" style=\"width: ").append(String.format("%.1f", compliancePercentage)).append("%;\"></div>\n");
            html.append("        </div>\n");
        }

        html.append("    </div>\n");

        // Pages table
        html.append("    <h2>Page Analysis</h2>\n");
        html.append("    <table>\n");
        html.append("        <tr>\n");
        html.append("            <th>Page</th>\n");
        html.append("            <th>Violations</th>\n");
        html.append("            <th>Fixes Applied</th>\n");
        html.append("            <th>Compliance Improvement</th>\n");
        html.append("            <th>Report</th>\n");
        html.append("        </tr>\n");

        for (Map.Entry<String, Integer> entry : summary.getViolationsByPage().entrySet()) {
            String filePath = entry.getKey();
            int violations = entry.getValue();
            RemediationResult result = results.get(filePath);
            int fixes = result != null ? result.getFixesApplied() : 0;

            double compliancePercentage = violations > 0 ? (double) fixes / violations * 100 : 100;
            String fileName = new File(filePath).getName();
            String reportName = "report_" + fileName;

            html.append("        <tr>\n");
            html.append("            <td>").append(fileName).append("</td>\n");
            html.append("            <td>").append(violations).append("</td>\n");
            html.append("            <td>").append(fixes).append("</td>\n");
            html.append("            <td>").append(String.format("%.1f", compliancePercentage)).append("%</td>\n");
            html.append("            <td><a href=\"").append(reportName).append("\">View Report</a></td>\n");
            html.append("        </tr>\n");
        }

        html.append("    </table>\n");

        // Top violations
        if (!results.isEmpty()) {
            html.append("    <h2>Top WCAG Violations</h2>\n");

            // Count violations by rule ID
            Map<String, Integer> violationsByRule = new HashMap<>();
            Map<String, String> ruleDescriptions = new HashMap<>();

            for (RemediationResult result : results.values()) {
                for (Violation violation : result.getViolations()) {
                    String ruleId = violation.getRuleId();
                    violationsByRule.put(ruleId, violationsByRule.getOrDefault(ruleId, 0) + 1);
                    ruleDescriptions.putIfAbsent(ruleId, violation.getMessage());
                }
            }

            // Sort by frequency
            List<Map.Entry<String, Integer>> sortedViolations = new ArrayList<>(violationsByRule.entrySet());
            sortedViolations.sort(Map.Entry.<String, Integer>comparingByValue().reversed());

            html.append("    <table>\n");
            html.append("        <tr>\n");
            html.append("            <th>Rule ID</th>\n");
            html.append("            <th>Description</th>\n");
            html.append("            <th>Occurrences</th>\n");
            html.append("        </tr>\n");

            for (Map.Entry<String, Integer> entry : sortedViolations) {
                html.append("        <tr>\n");
                html.append("            <td>").append(entry.getKey()).append("</td>\n");
                html.append("            <td>").append(ruleDescriptions.getOrDefault(entry.getKey(), "")).append("</td>\n");
                html.append("            <td>").append(entry.getValue()).append("</td>\n");
                html.append("        </tr>\n");
            }

            html.append("    </table>\n");
        }

        html.append("</body>\n");
        html.append("</html>");

        Files.writeString(Paths.get(reportPath), html.toString());
        LOGGER.info("Generated summary report: " + reportPath);
    }

    /**
     * Print summary to console
     */
    private static void printSummary(SummaryReport summary) {
        System.out.println("\n=== WCAG Compliance Summary ===");
        System.out.println("Total pages analyzed: " + summary.getTotalPages());
        System.out.println("Total violations found: " + summary.getTotalViolations());
        System.out.println("Total fixes applied: " + summary.getTotalFixesApplied());

        if (summary.getTotalViolations() > 0) {
            double compliancePercentage = (double) summary.getTotalFixesApplied() / summary.getTotalViolations() * 100;
            System.out.println("Automated compliance improvement: " + String.format("%.1f", compliancePercentage) + "%");
        }

        System.out.println("\nAnalysis complete. See reports in the output directory.");
    }

    /**
     * Escape HTML special characters
     */
    private static String escapeHtml(String input) {
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
