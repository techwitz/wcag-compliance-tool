package com.techwitz.accessibility;

import com.techwitz.accessibility.report.SummaryReport;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * Main application class for WCAG 2.3 Compliance Tool
 * Handles scanning URLs and local file folders for accessibility issues
 */
public class WcagComplianceApplication {
    private static final Logger LOGGER = Logger.getLogger(WcagComplianceApplication.class.getName());
    private final WcagComplianceTool complianceTool;

    /**
     * Constructor with default configuration
     */
    public WcagComplianceApplication() {
        ComplianceConfig config = new ComplianceConfig.Builder()
                .setIncludeLevel2Rules(true)
                .setIncludeLevel3Rules(false)
                .build();

        this.complianceTool = new WcagComplianceTool(config);
    }

    /**
     * Constructor with custom configuration
     * @param config Custom compliance configuration
     */
    public WcagComplianceApplication(ComplianceConfig config) {
        this.complianceTool = new WcagComplianceTool(config);
    }

    /**
     * Main method for command-line execution
     * @param args Command-line arguments
     */
    public static void main(String[] args) {
        try {
            // Parse command-line arguments
            CommandLineOptions options = parseArguments(args);

            // Create configuration based on options
            ComplianceConfig config = createConfig(options);

            // Create application instance
            WcagComplianceApplication app = new WcagComplianceApplication(config);

            // Process based on input type
            if (options.getUrl() != null) {
                // Process URL
                app.processUrl(options.getUrl(), options.getOutputDir(), options.isApplyFixes());
            } else if (options.getSourceDir() != null) {
                // Process folder
                app.processFolder(options.getSourceDir(), options.getOutputDir(), options.isApplyFixes(),
                                  options.getIncludePatterns(), options.getExcludePatterns());
            } else {
                System.out.println("Please provide either a URL or a source directory to analyze.");
                printUsage();
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error running application", e);
            System.err.println("Error: " + e.getMessage());
            printUsage();
        }
    }

    /**
     * Parse command-line arguments
     * @param args Command-line arguments
     * @return CommandLineOptions object with parsed options
     */
    private static CommandLineOptions parseArguments(String[] args) {
        CommandLineOptions options = new CommandLineOptions();

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--url":
                    if (i + 1 < args.length) options.setUrl(args[++i]);
                    break;
                case "--source-dir":
                    if (i + 1 < args.length) options.setSourceDir(args[++i]);
                    break;
                case "--output-dir":
                    if (i + 1 < args.length) options.setOutputDir(args[++i]);
                    break;
                case "--apply-fixes":
                    options.setApplyFixes(true);
                    break;
                case "--include-aa":
                    options.setIncludeAA(true);
                    break;
                case "--include-aaa":
                    options.setIncludeAAA(true);
                    break;
                case "--include":
                    if (i + 1 < args.length) options.addIncludePattern(args[++i]);
                    break;
                case "--exclude":
                    if (i + 1 < args.length) options.addExcludePattern(args[++i]);
                    break;
                case "--help":
                    printUsage();
                    System.exit(0);
                default:
                    System.err.println("Unknown option: " + args[i]);
                    printUsage();
                    System.exit(1);
            }
        }

        return options;
    }

    /**
     * Print usage instructions
     */
    private static void printUsage() {
        System.out.println("WCAG 2.3 Compliance Tool");
        System.out.println("Usage:");
        System.out.println("  --url <url>              URL to analyze");
        System.out.println("  --source-dir <path>      Directory containing source files to analyze");
        System.out.println("  --output-dir <path>      Directory for output reports (default: ./wcag-reports)");
        System.out.println("  --apply-fixes            Apply automatic fixes to issues");
        System.out.println("  --include-aa             Include WCAG 2.3 Level AA checks");
        System.out.println("  --include-aaa            Include WCAG 2.3 Level AAA checks");
        System.out.println("  --include <pattern>      Include files matching pattern (glob format)");
        System.out.println("  --exclude <pattern>      Exclude files matching pattern (glob format)");
        System.out.println("  --help                   Display this help message");
        System.out.println("");
        System.out.println("Examples:");
        System.out.println("  Analyze a URL:           java -jar wcag-tool.jar --url https://example.com");
        System.out.println("  Analyze and fix folder:  java -jar wcag-tool.jar --source-dir ./src --apply-fixes");
    }

    /**
     * Create configuration based on command-line options
     * @param options Command-line options
     * @return ComplianceConfig object
     */
    private static ComplianceConfig createConfig(CommandLineOptions options) {
        ComplianceConfig.Builder builder = new ComplianceConfig.Builder()
                .setIncludeLevel2Rules(options.isIncludeAA())
                .setIncludeLevel3Rules(options.isIncludeAAA());

        // Add common CSS properties to fix contrast issues
        builder.setCssProperty(".text-light", "color: #000000;")
                .setCssProperty(".btn-light", "color: #000000; background-color: #f8f9fa;")
                .setCssProperty(".navbar-light", "color: #000000;")
                .setCssProperty(".text-white", "color: #000000;")
                .setCssProperty("a", "color: #0056b3;");

        // Add default excluded paths
        builder.addExcludedPage("**/node_modules/**")
                .addExcludedPage("**/vendor/**")
                .addExcludedPage("**/dist/**")
                .addExcludedPage("**/build/**");

        return builder.build();
    }

    /**
     * Process a URL for WCAG compliance
     * @param url URL to process
     * @param outputDir Output directory for reports
     * @param applyFixes Whether to apply automatic fixes
     * @throws IOException If an I/O error occurs
     */
    public void processUrl(String url, String outputDir, boolean applyFixes) throws IOException {
        System.out.println("Processing URL: " + url);

        // Create output directory if it doesn't exist
        createOutputDirectory(outputDir);

        // Process the URL
        RemediationOptions options = new RemediationOptions.Builder()
                .setAutoFix(applyFixes)
                .setApplyAriaEnhancements(true)
                .setFixContrastIssues(true)
                .build();

        String outputFilePath = null;
        if (applyFixes) {
            String fileName = getFileNameFromUrl(url);
            outputFilePath = Paths.get(outputDir, "fixed_" + fileName).toString() + ".html";
            options = new RemediationOptions.Builder(options)
                    .setSaveToPath(outputFilePath)
                    .build();
        }

        // Analyze the URL
        RemediationResult result = complianceTool.remediatePage(url, options);

        // Generate report
        String reportPath = Paths.get(outputDir, "report_" + getFileNameFromUrl(url)).toString() + ".html";
        generateHtmlReport(result, reportPath);

        // Print summary
        printResultSummary(result, outputFilePath);
    }

    /**
     * Process a folder containing source code files
     * @param sourceDir Source directory to process
     * @param outputDir Output directory for reports
     * @param applyFixes Whether to apply automatic fixes
     * @param includePatterns Patterns of files to include
     * @param excludePatterns Patterns of files to exclude
     * @throws IOException If an I/O error occurs
     */
    public void processFolder(String sourceDir, String outputDir, boolean applyFixes,
                              List<String> includePatterns, List<String> excludePatterns) throws IOException {
        System.out.println("Processing folder: " + sourceDir);

        // Create output directory if it doesn't exist
        createOutputDirectory(outputDir);

        // Find all relevant files in the directory
        List<Path> filesToProcess = findFilesToProcess(sourceDir, includePatterns, excludePatterns);
        System.out.println("Found " + filesToProcess.size() + " files to process");

        if (filesToProcess.isEmpty()) {
            System.out.println("No files to process. Check include/exclude patterns.");
            return;
        }

        // Process each file in parallel
        int numThreads = Math.min(Runtime.getRuntime().availableProcessors(), filesToProcess.size());
        System.out.println("Using " + numThreads + " threads for parallel processing");

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        List<Future<RemediationResult>> futures = new ArrayList<>();
        Map<Path, Future<RemediationResult>> resultsByPath = new HashMap<>();

        for (Path filePath : filesToProcess) {
            Future<RemediationResult> future = executor.submit(() -> processFile(filePath, outputDir, applyFixes));
            futures.add(future);
            resultsByPath.put(filePath, future);
        }

        // Wait for all tasks to complete
        Map<String, RemediationResult> results = new HashMap<>();
        int totalViolations = 0;
        int totalFixesApplied = 0;

        for (Map.Entry<Path, Future<RemediationResult>> entry : resultsByPath.entrySet()) {
            try {
                Path path = entry.getKey();
                RemediationResult result = entry.getValue().get();

                results.put(path.toString(), result);
                totalViolations += result.getViolations().size();
                totalFixesApplied += result.getFixesApplied();

                printFileResultSummary(path.toString(), result);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error processing file", e);
            }
        }

        executor.shutdown();

        // Generate summary report
        SummaryReport summary = new SummaryReport.Builder()
                .setTotalPages(results.size())
                .setTotalViolations(totalViolations)
                .setTotalFixesApplied(totalFixesApplied)
                .setViolationsByPage(results.entrySet().stream()
                                             .collect(Collectors.toMap(
                                                     Map.Entry::getKey,
                                                     e -> e.getValue().getViolations().size())))
                .build();

        String summaryReportPath = Paths.get(outputDir, "summary_report.html").toString();
        generateHtmlSummaryReport(summary, results, summaryReportPath);

        System.out.println("\n=== WCAG Compliance Summary ===");
        System.out.println("Total files processed: " + results.size());
        System.out.println("Total violations found: " + totalViolations);
        System.out.println("Total fixes applied: " + totalFixesApplied);

        if (totalViolations > 0) {
            double compliancePercentage = (double) totalFixesApplied / totalViolations * 100;
            System.out.println("Automated compliance improvement: " + String.format("%.1f", compliancePercentage) + "%");
        }

        System.out.println("\nAnalysis complete. See reports in: " + outputDir);
    }

    /**
     * Process a single file
     * @param filePath Path to the file
     * @param outputDir Output directory for reports
     * @param applyFixes Whether to apply automatic fixes
     * @return RemediationResult for the file
     */
    private RemediationResult processFile(Path filePath, String outputDir, boolean applyFixes) throws IOException {
        LOGGER.info("Processing file: " + filePath);

        String fileContent = Files.readString(filePath, StandardCharsets.UTF_8);
        Document document;

        // Detect file type based on extension
        String fileName = filePath.getFileName().toString().toLowerCase();
        if (fileName.endsWith(".html") || fileName.endsWith(".htm") ||
                fileName.endsWith(".xhtml") || fileName.endsWith(".jsp")) {
            // Parse as HTML
            document = Jsoup.parse(fileContent);
        } else {
            // For non-HTML files, try to extract HTML content
            document = extractHtmlFromFile(filePath, fileContent);

            if (document == null) {
                LOGGER.warning("Skipping non-HTML file or file with no HTML content: " + filePath);
                return createEmptyResult(filePath.toString());
            }
        }

        // Set base URI to help with resolving relative links
        document.setBaseUri(filePath.toUri().toString());

        // Create remediation options
        RemediationOptions options = new RemediationOptions.Builder()
                .setAutoFix(applyFixes)
                .setApplyAriaEnhancements(true)
                .setFixContrastIssues(true)
                .build();

        if (applyFixes) {
            // Setup output path for fixed file
            options = new RemediationOptions.Builder(options)
                    .setSaveToPath(filePath.toString())
                    .build();
        }

        // Evaluate and remediate
        List<Violation> violations = complianceTool.getRuleEngine().evaluateRules(document);

        // If no violations, create an empty result
        if (violations.isEmpty()) {
            LOGGER.info("No violations found in file: " + filePath);
            return createEmptyResult(filePath.toString());
        }

        RemediationResult result = complianceTool.getRemediationService().applyFixes(document, violations, options);

        // Generate report for this file
        String reportFileName = "report_" + filePath.getFileName().toString() + ".html";
        String reportPath = Paths.get(outputDir, reportFileName).toString();
        generateHtmlReport(result, reportPath);

        return result;
    }

    /**
     * Create an empty result for files with no violations
     * @param filePath Path to the file
     * @return Empty RemediationResult
     */
    private RemediationResult createEmptyResult(String filePath) {
        return new RemediationResult.Builder()
                .setPageUrl(filePath)
                .setOriginalHtml("")
                .setRemediatedHtml("")
                .setViolations(Collections.emptyList())
                .setFixesApplied(0)
                .setChangeLog(Collections.emptyList())
                .build();
    }

    /**
     * Extract HTML content from non-HTML files (JSP, JavaScript, etc.)
     * @param filePath Path to the file
     * @param fileContent Content of the file
     * @return Document containing extracted HTML, or null if no HTML found
     */
    private Document extractHtmlFromFile(Path filePath, String fileContent) {
        String fileName = filePath.getFileName().toString().toLowerCase();

        if (fileName.endsWith(".js") || fileName.endsWith(".jsx") || fileName.endsWith(".tsx")) {
            // Extract HTML from JavaScript/JSX/TSX files
            // Look for JSX components, template literals with HTML, etc.
            return extractHtmlFromJavaScript(fileContent);
        } else if (fileName.endsWith(".java")) {
            // Extract HTML from Java files
            // Look for HTML in string literals, JSP scriptlets, etc.
            return extractHtmlFromJava(fileContent);
        } else if (fileName.endsWith(".jsp") || fileName.endsWith(".jspf")) {
            // Parse JSP files directly as HTML
            return Jsoup.parse(fileContent);
        } else if (fileName.endsWith(".css")) {
            // CSS files don't contain HTML structure, but we can check for contrast issues
            return createDummyHtmlForCss(fileContent);
        } else {
            // Unknown file type, try generic extraction
            return extractGenericHtml(fileContent);
        }
    }

    /**
     * Extract HTML from JavaScript files
     * @param fileContent JavaScript file content
     * @return Document with extracted HTML
     */
    private Document extractHtmlFromJavaScript(String fileContent) {
        StringBuilder htmlContent = new StringBuilder("<html><body>");

        // Extract JSX components
        // Look for patterns like: return (<div>...</div>)
        // This is a simplified approach and would need a proper parser for production

        // Look for template literals with HTML content
        // For example: `<div class="container">...</div>`
        int startIndex = 0;
        while ((startIndex = fileContent.indexOf('`', startIndex)) >= 0) {
            int endIndex = fileContent.indexOf('`', startIndex + 1);
            if (endIndex > startIndex) {
                String potentialHtml = fileContent.substring(startIndex + 1, endIndex);
                if (potentialHtml.contains("<") && potentialHtml.contains(">")) {
                    htmlContent.append(potentialHtml).append("\n");
                }
                startIndex = endIndex + 1;
            } else {
                break;
            }
        }

        // Look for HTML strings
        // For example: '<div class="container">...</div>'
        extractStringLiterals(fileContent, htmlContent);

        htmlContent.append("</body></html>");

        // If no HTML content found, return null
        if (htmlContent.toString().equals("<html><body></body></html>")) {
            return null;
        }

        return Jsoup.parse(htmlContent.toString());
    }

    /**
     * Extract HTML from Java files
     * @param fileContent Java file content
     * @return Document with extracted HTML
     */
    private Document extractHtmlFromJava(String fileContent) {
        StringBuilder htmlContent = new StringBuilder("<html><body>");

        // Extract string literals that might contain HTML
        extractStringLiterals(fileContent, htmlContent);

        htmlContent.append("</body></html>");

        // If no HTML content found, return null
        if (htmlContent.toString().equals("<html><body></body></html>")) {
            return null;
        }

        return Jsoup.parse(htmlContent.toString());
    }

    /**
     * Extract string literals that might contain HTML
     * @param fileContent File content
     * @param htmlContent StringBuilder to append extracted HTML
     */
    private void extractStringLiterals(String fileContent, StringBuilder htmlContent) {
        // Look for double-quoted strings
        int startIndex = 0;
        while ((startIndex = fileContent.indexOf('"', startIndex)) >= 0) {
            // Make sure we're not in a commented section
            if (isInComment(fileContent, startIndex)) {
                startIndex++;
                continue;
            }

            // Find the end of the string
            int endIndex = -1;
            for (int i = startIndex + 1; i < fileContent.length(); i++) {
                if (fileContent.charAt(i) == '"' && fileContent.charAt(i - 1) != '\\') {
                    endIndex = i;
                    break;
                }
            }

            if (endIndex > startIndex) {
                String potentialHtml = fileContent.substring(startIndex + 1, endIndex);
                if (potentialHtml.contains("<") && potentialHtml.contains(">")) {
                    htmlContent.append(potentialHtml).append("\n");
                }
                startIndex = endIndex + 1;
            } else {
                break;
            }
        }

        // Look for single-quoted strings
        startIndex = 0;
        while ((startIndex = fileContent.indexOf('\'', startIndex)) >= 0) {
            // Make sure we're not in a commented section
            if (isInComment(fileContent, startIndex)) {
                startIndex++;
                continue;
            }

            // Find the end of the string
            int endIndex = -1;
            for (int i = startIndex + 1; i < fileContent.length(); i++) {
                if (fileContent.charAt(i) == '\'' && fileContent.charAt(i - 1) != '\\') {
                    endIndex = i;
                    break;
                }
            }

            if (endIndex > startIndex) {
                String potentialHtml = fileContent.substring(startIndex + 1, endIndex);
                if (potentialHtml.contains("<") && potentialHtml.contains(">")) {
                    htmlContent.append(potentialHtml).append("\n");
                }
                startIndex = endIndex + 1;
            } else {
                break;
            }
        }
    }

    /**
     * Check if position is inside a comment
     * @param content File content
     * @param position Position to check
     * @return true if position is in a comment
     */
    private boolean isInComment(String content, int position) {
        // Check for single-line comment
        int lineStart = content.lastIndexOf('\n', position);
        if (lineStart == -1) lineStart = 0;

        int commentStart = content.indexOf("//", lineStart);
        if (commentStart != -1 && commentStart < position) {
            return true;
        }

        // Check for multi-line comment
        int lastCommentStart = content.lastIndexOf("/*", position);
        if (lastCommentStart != -1) {
            int lastCommentEnd = content.lastIndexOf("*/", position);
            if (lastCommentEnd < lastCommentStart) {
                return true;
            }
        }

        return false;
    }

    /**
     * Create a dummy HTML document for CSS files
     * @param cssContent CSS file content
     * @return Document with dummy HTML containing the CSS
     */
    private Document createDummyHtmlForCss(String cssContent) {
        StringBuilder htmlContent = new StringBuilder();
        htmlContent.append("<!DOCTYPE html>\n");
        htmlContent.append("<html>\n");
        htmlContent.append("<head>\n");
        htmlContent.append("<style type=\"text/css\">\n");
        htmlContent.append(cssContent);
        htmlContent.append("\n</style>\n");
        htmlContent.append("</head>\n");
        htmlContent.append("<body>\n");
        htmlContent.append("<div class=\"container\">\n");
        htmlContent.append("  <p>Sample text for contrast checking</p>\n");
        htmlContent.append("</div>\n");
        htmlContent.append("</body>\n");
        htmlContent.append("</html>");

        return Jsoup.parse(htmlContent.toString());
    }

    /**
     * Extract HTML from generic files
     * @param fileContent File content
     * @return Document with extracted HTML
     */
    private Document extractGenericHtml(String fileContent) {
        // Simple approach: look for content between < and > tags
        StringBuilder htmlContent = new StringBuilder();
        htmlContent.append("<html><body>");

        int index = 0;
        while (index < fileContent.length()) {
            int openTag = fileContent.indexOf('<', index);
            if (openTag == -1) break;

            int closeTag = fileContent.indexOf('>', openTag);
            if (closeTag == -1) break;

            // Check if this looks like an HTML tag
            String tag = fileContent.substring(openTag, closeTag + 1);
            if (tag.matches("</?[a-zA-Z][^>]*>")) {
                // Find the content
                int tagEnd = closeTag + 1;
                int nextOpen = fileContent.indexOf('<', tagEnd);

                if (nextOpen > tagEnd) {
                    htmlContent.append(tag);
                    htmlContent.append(fileContent.substring(tagEnd, nextOpen));
                    index = nextOpen;
                } else {
                    htmlContent.append(tag);
                    index = tagEnd;
                }
            } else {
                index = closeTag + 1;
            }
        }

        htmlContent.append("</body></html>");

        // If no HTML content found, return null
        if (htmlContent.toString().equals("<html><body></body></html>")) {
            return null;
        }

        return Jsoup.parse(htmlContent.toString());
    }

    /**
     * Find files to process in the source directory
     * @param sourceDir Source directory
     * @param includePatterns Patterns to include
     * @param excludePatterns Patterns to exclude
     * @return List of file paths to process
     */
    private List<Path> findFilesToProcess(String sourceDir, List<String> includePatterns, List<String> excludePatterns)
            throws IOException {

        Path sourcePath = Paths.get(sourceDir);

        // If no include patterns specified, default to HTML, JSP, and other web files
        if (includePatterns.isEmpty()) {
            includePatterns.add("**/*.html");
            includePatterns.add("**/*.htm");
            includePatterns.add("**/*.jsp");
            includePatterns.add("**/*.jsf");
            includePatterns.add("**/*.xhtml");
        }

        LOGGER.info("Include patterns: " + String.join(", ", includePatterns));
        LOGGER.info("Exclude patterns: " + String.join(", ", excludePatterns));

        // Find all files matching include patterns and not matching exclude patterns
        List<Path> filesToProcess = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(sourcePath)) {
            filesToProcess = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        // Convert to a relative path for pattern matching
                        Path relativePath = sourcePath.relativize(path);
                        String relativePathStr = relativePath.toString().replace('\\', '/');

                        // Check if the file matches any include pattern
                        boolean included = includePatterns.isEmpty() ||
                                includePatterns.stream()
                                        .anyMatch(pattern -> pathMatchesPattern(relativePathStr, pattern));

                        // Check if the file matches any exclude pattern
                        boolean excluded = !excludePatterns.isEmpty() &&
                                excludePatterns.stream()
                                        .anyMatch(pattern -> pathMatchesPattern(relativePathStr, pattern));

                        return included && !excluded;
                    })
                    .collect(Collectors.toList());
        }

        return filesToProcess;
    }

    /**
     * Check if a path matches a glob pattern
     * @param path File path
     * @param pattern Glob pattern
     * @return true if path matches pattern, false otherwise
     */
    private boolean pathMatchesPattern(String path, String pattern) {
        // Convert glob pattern to regex
        pattern = pattern.replace(".", "\\.");
        pattern = pattern.replace("**", "__DOUBLE_STAR__");
        pattern = pattern.replace("*", "[^/]*");
        pattern = pattern.replace("?", ".");
        pattern = pattern.replace("__DOUBLE_STAR__", ".*");

        // Add start/end markers
        if (!pattern.startsWith("^")) {
            pattern = "^" + pattern;
        }
        if (!pattern.endsWith("$")) {
            pattern = pattern + "$";
        }

        return path.matches(pattern);
    }

    /**
     * Generate HTML report for a single file result
     * @param result Remediation result
     * @param reportPath Output path for the report
     */
    private void generateHtmlReport(RemediationResult result, String reportPath) throws IOException {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"en\">\n");
        html.append("<head>\n");
        html.append("    <meta charset=\"UTF-8\">\n");
        html.append("    <title>WCAG 2.3 Compliance Report</title>\n");
        html.append("    <style>\n");
        html.append("        body { font-family: Arial, sans-serif; line-height: 1.6; max-width: 1200px; margin: 0 auto; padding: 20px; }\n");
        html.append("        h1, h2 { color: #333; }\n");
        html.append("        .summary { background: #f5f5f5; padding: 15px; border-radius: 5px; margin-bottom: 20px; }\n");
        html.append("        .violation { background: #fff8f8; border-left: 4px solid #ff6b6b; padding: 15px; margin-bottom: 10px; }\n");
        html.append("        .fixed { background: #f0fff0; border-left: 4px solid #4caf50; }\n");
        html.append("        .violation h3 { margin-top: 0; color: #d32f2f; }\n");
        html.append("        .fixed h3 { color: #388e3c; }\n");
        html.append("        pre { background: #f8f8f8; padding: 10px; overflow: auto; max-height: 300px; }\n");
        html.append("        .critical { color: #d32f2f; }\n");
        html.append("        .serious { color: #f57c00; }\n");
        html.append("        .moderate { color: #fbc02d; }\n");
        html.append("        .minor { color: #7cb342; }\n");
        html.append("        .diff-container { display: flex; flex-direction: row; margin-bottom: 15px; }\n");
        html.append("        .diff-original, .diff-fixed { flex: 1; margin: 0 10px; }\n");
        html.append("        .diff-header { font-weight: bold; margin-bottom: 5px; }\n");
        html.append("        .tabs { display: flex; margin-bottom: 10px; }\n");
        html.append("        .tab { padding: 8px 15px; cursor: pointer; border: 1px solid #ccc; background: #f1f1f1; }\n");
        html.append("        .tab.active { background: #fff; border-bottom: none; }\n");
        html.append("        .tab-content { display: none; }\n");
        html.append("        .tab-content.active { display: block; }\n");
        html.append("    </style>\n");
        html.append("    <script>\n");
        html.append("        function switchTab(tabId) {\n");
        html.append("            // Hide all tab contents\n");
        html.append("            document.querySelectorAll('.tab-content').forEach(tab => {\n");
        html.append("                tab.classList.remove('active');\n");
        html.append("            });\n");
        html.append("            \n");
        html.append("            // Deactivate all tabs\n");
        html.append("            document.querySelectorAll('.tab').forEach(tab => {\n");
        html.append("                tab.classList.remove('active');\n");
        html.append("            });\n");
        html.append("            \n");
        html.append("            // Show the selected tab content\n");
        html.append("            document.getElementById(tabId).classList.add('active');\n");
        html.append("            \n");
        html.append("            // Activate the selected tab\n");
        html.append("            document.querySelector(`[onclick=\"switchTab('${tabId}')\"]`).classList.add('active');\n");
        html.append("        }\n");
        html.append("    </script>\n");
        html.append("</head>\n");
        html.append("<body>\n");

        // Header
        html.append("    <h1>WCAG 2.3 Compliance Report</h1>\n");
        html.append("    <p>File: ").append(escapeHtml(result.getPageUrl())).append("</p>\n");

        // Summary
        html.append("    <div class=\"summary\">\n");
        html.append("        <h2>Summary</h2>\n");
        html.append("        <p>Total violations: ").append(result.getViolations().size()).append("</p>\n");
        html.append("        <p>Fixes applied: ").append(result.getFixesApplied()).append("</p>\n");

        if (result.getViolations().size() > 0 && result.getFixesApplied() > 0) {
            double compliancePercentage = (double) result.getFixesApplied() / result.getViolations().size() * 100;
            html.append("        <p>Automated compliance improvement: ").append(String.format("%.1f", compliancePercentage)).append("%</p>\n");
        }

        html.append("    </div>\n");

        // Tabs for violations and before/after
        html.append("    <div class=\"tabs\">\n");
        html.append("        <div class=\"tab active\" onclick=\"switchTab('violations-tab')\">Violations</div>\n");

        if (result.getFixesApplied() > 0) {
            html.append("        <div class=\"tab\" onclick=\"switchTab('before-after-tab')\">Before/After</div>\n");
        }

        html.append("    </div>\n");

        // Violations tab
        html.append("    <div id=\"violations-tab\" class=\"tab-content active\">\n");

        if (result.getViolations().isEmpty()) {
            html.append("    <p>No violations found. The page appears to be compliant with WCAG 2.3 standards.</p>\n");
        } else {
            // Group violations by severity
            Map<String, List<Violation>> violationsBySeverity = result.getViolations().stream()
                    .collect(Collectors.groupingBy(Violation::getSeverity));

            // Show critical violations first
            for (String severity : Arrays.asList("critical", "serious", "moderate", "minor")) {
                List<Violation> violationsForSeverity = violationsBySeverity.getOrDefault(severity, Collections.emptyList());
                if (!violationsForSeverity.isEmpty()) {
                    html.append("        <h2>").append(capitalize(severity)).append(" Violations (").append(violationsForSeverity.size()).append(")</h2>\n");

                    for (Violation violation : violationsForSeverity) {
                        String violationClass = "violation";
                        if (violation.isAutoFixable() && result.getFixesApplied() > 0) {
                            violationClass += " fixed";
                        }

                        html.append("        <div class=\"").append(violationClass).append("\">\n");
                        html.append("            <h3>").append(escapeHtml(violation.getMessage())).append("</h3>\n");
                        html.append("            <p>Rule: ").append(escapeHtml(violation.getRuleId())).append("</p>\n");
                        html.append("            <p>Auto-fixable: ").append(violation.isAutoFixable()).append("</p>\n");
                        html.append("            <p>Remediation: ").append(escapeHtml(violation.getRemediation())).append("</p>\n");
                        html.append("            <p>Element:</p>\n");
                        html.append("            <pre>").append(escapeHtml(violation.getElement())).append("</pre>\n");
                        html.append("        </div>\n");
                    }
                }
            }
        }

        html.append("    </div>\n");

        // Before/After tab
        if (result.getFixesApplied() > 0) {
            html.append("    <div id=\"before-after-tab\" class=\"tab-content\">\n");
            html.append("        <h2>HTML Comparison</h2>\n");

            // If the content is very large, just show a summary
            if (result.getOriginalHtml() != null && result.getOriginalHtml().length() > 10000) {
                html.append("        <p>The HTML content is too large to display in full. Summary of changes:</p>\n");

                // Change log
                if (!result.getChangeLog().isEmpty()) {
                    html.append("        <h3>Changes Applied</h3>\n");
                    html.append("        <ul>\n");
                    for (String change : result.getChangeLog()) {
                        html.append("            <li>").append(escapeHtml(change)).append("</li>\n");
                    }
                    html.append("        </ul>\n");
                }
            } else {
                // Show side-by-side comparison
                html.append("        <div class=\"diff-container\">\n");
                html.append("            <div class=\"diff-original\">\n");
                html.append("                <div class=\"diff-header\">Original HTML</div>\n");
                html.append("                <pre>").append(escapeHtml(result.getOriginalHtml())).append("</pre>\n");
                html.append("            </div>\n");
                html.append("            <div class=\"diff-fixed\">\n");
                html.append("                <div class=\"diff-header\">Fixed HTML</div>\n");
                html.append("                <pre>").append(escapeHtml(result.getRemediatedHtml())).append("</pre>\n");
                html.append("            </div>\n");
                html.append("        </div>\n");
            }

            html.append("    </div>\n");
        }

        // Change log in the violations tab
        if (!result.getChangeLog().isEmpty()) {
            html.append("    <div id=\"violations-tab\" class=\"tab-content active\">\n");
            html.append("        <h2>Changes Applied</h2>\n");
            html.append("        <ul>\n");
            for (String change : result.getChangeLog()) {
                html.append("            <li>").append(escapeHtml(change)).append("</li>\n");
            }
            html.append("        </ul>\n");
            html.append("    </div>\n");
        }

        html.append("</body>\n");
        html.append("</html>");

        Files.writeString(Paths.get(reportPath), html.toString());
        LOGGER.info("Generated report: " + reportPath);
    }

    /**
     * Generate HTML summary report for multiple files
     * @param summary Summary report
     * @param results Map of path to remediation results
     * @param reportPath Output path for the report
     */
    private void generateHtmlSummaryReport(SummaryReport summary,
                                           Map<String, RemediationResult> results,
                                           String reportPath) throws IOException {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"en\">\n");
        html.append("<head>\n");
        html.append("    <meta charset=\"UTF-8\">\n");
        html.append("    <title>WCAG 2.3 Compliance Summary Report</title>\n");
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
        html.append("        .chart-container { width: 600px; height: 400px; margin: 0 auto; }\n");
        html.append("    </style>\n");
        html.append("    <script src=\"https://cdn.jsdelivr.net/npm/chart.js\"></script>\n");
        html.append("</head>\n");
        html.append("<body>\n");

        // Header
        html.append("    <h1>WCAG 2.3 Compliance Summary Report</h1>\n");
        html.append("    <p>Generated on: ").append(new Date()).append("</p>\n");

        // Summary
        html.append("    <div class=\"summary\">\n");
        html.append("        <h2>Summary</h2>\n");
        html.append("        <p>Total files analyzed: ").append(summary.getTotalPages()).append("</p>\n");
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

        // Chart for visualization
        html.append("    <h2>Violations by Category</h2>\n");
        html.append("    <div class=\"chart-container\">\n");
        html.append("        <canvas id=\"violations-chart\"></canvas>\n");
        html.append("    </div>\n");

        // JavaScript for chart
        html.append("    <script>\n");
        html.append("        document.addEventListener('DOMContentLoaded', function() {\n");
        html.append("            const ctx = document.getElementById('violations-chart').getContext('2d');\n");

        // Count violations by rule
        Map<String, Integer> violationsByRule = new HashMap<>();
        for (RemediationResult result : results.values()) {
            for (Violation violation : result.getViolations()) {
                String ruleId = violation.getRuleId();
                violationsByRule.put(ruleId, violationsByRule.getOrDefault(ruleId, 0) + 1);
            }
        }

        // Convert to arrays for Chart.js
        List<Map.Entry<String, Integer>> sortedViolations = new ArrayList<>(violationsByRule.entrySet());
        sortedViolations.sort(Map.Entry.<String, Integer>comparingByValue().reversed());

        // Take top 10 for chart
        List<Map.Entry<String, Integer>> topViolations = sortedViolations.stream()
                .limit(10)
                .collect(Collectors.toList());

        // Create labels and data arrays
        html.append("            const labels = [");
        for (int i = 0; i < topViolations.size(); i++) {
            if (i > 0) html.append(", ");
            html.append("'").append(escapeHtml(topViolations.get(i).getKey())).append("'");
        }
        html.append("];\n");

        html.append("            const data = [");
        for (int i = 0; i < topViolations.size(); i++) {
            if (i > 0) html.append(", ");
            html.append(topViolations.get(i).getValue());
        }
        html.append("];\n");

        html.append("            new Chart(ctx, {\n");
        html.append("                type: 'bar',\n");
        html.append("                data: {\n");
        html.append("                    labels: labels,\n");
        html.append("                    datasets: [{\n");
        html.append("                        label: 'Number of Violations',\n");
        html.append("                        data: data,\n");
        html.append("                        backgroundColor: 'rgba(54, 162, 235, 0.5)',\n");
        html.append("                        borderColor: 'rgba(54, 162, 235, 1)',\n");
        html.append("                        borderWidth: 1\n");
        html.append("                    }]\n");
        html.append("                },\n");
        html.append("                options: {\n");
        html.append("                    scales: {\n");
        html.append("                        y: {\n");
        html.append("                            beginAtZero: true\n");
        html.append("                        }\n");
        html.append("                    }\n");
        html.append("                }\n");
        html.append("            });\n");
        html.append("        });\n");
        html.append("    </script>\n");

        // Files table
        html.append("    <h2>File Analysis</h2>\n");
        html.append("    <table>\n");
        html.append("        <tr>\n");
        html.append("            <th>File</th>\n");
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
            String reportName = "report_" + fileName + ".html";

            html.append("        <tr>\n");
            html.append("            <td>").append(escapeHtml(fileName)).append("</td>\n");
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
            Map<String, Integer> violationCounts = new HashMap<>();
            Map<String, String> ruleDescriptions = new HashMap<>();

            for (RemediationResult result : results.values()) {
                for (Violation violation : result.getViolations()) {
                    String ruleId = violation.getRuleId();
                    violationCounts.put(ruleId, violationCounts.getOrDefault(ruleId, 0) + 1);
                    ruleDescriptions.putIfAbsent(ruleId, violation.getMessage());
                }
            }

            // Sort by frequency
            List<Map.Entry<String, Integer>> sortedViolations2 = new ArrayList<>(violationCounts.entrySet());
            sortedViolations.sort(Map.Entry.<String, Integer>comparingByValue().reversed());

            html.append("    <table>\n");
            html.append("        <tr>\n");
            html.append("            <th>Rule ID</th>\n");
            html.append("            <th>Description</th>\n");
            html.append("            <th>Occurrences</th>\n");
            html.append("        </tr>\n");

            for (Map.Entry<String, Integer> entry : sortedViolations2) {
                html.append("        <tr>\n");
                html.append("            <td>").append(escapeHtml(entry.getKey())).append("</td>\n");
                html.append("            <td>").append(escapeHtml(ruleDescriptions.getOrDefault(entry.getKey(), ""))).append("</td>\n");
                html.append("            <td>").append(entry.getValue()).append("</td>\n");
                html.append("        </tr>\n");
            }

            html.append("    </table>\n");
        }

        // Recommendations section
        html.append("    <h2>Recommendations</h2>\n");
        html.append("    <p>Based on the analysis, here are the key areas to focus on for improving accessibility:</p>\n");
        html.append("    <ol>\n");

        // Get top 5 rule violations
        List<Map.Entry<String, Integer>> topViolations2 = new ArrayList<>(violationsByRule.entrySet());
        topViolations.sort(Map.Entry.<String, Integer>comparingByValue().reversed());

        Map<String, String> recommendationMessages = new HashMap<>();
        recommendationMessages.put("1.1.1-img-alt", "Add alternative text to all images to make them accessible to screen readers");
        recommendationMessages.put("1.3.1-form-label", "Ensure all form fields have properly associated labels");
        recommendationMessages.put("1.3.1-heading-structure", "Fix heading structure to follow proper hierarchical order");
        recommendationMessages.put("2.4.4-link-purpose", "Improve link text to clearly indicate the purpose of each link");
        recommendationMessages.put("1.3.1-table-headers", "Add proper headers to data tables for better screen reader navigation");
        recommendationMessages.put("1.4.3-color-contrast", "Increase color contrast ratios to ensure text is readable");
        recommendationMessages.put("2.1.1-keyboard-access", "Make all functionality accessible via keyboard");
        recommendationMessages.put("3.1.1-language", "Add language attributes to specify the document language");
        recommendationMessages.put("3.3.1-error-identification", "Enhance form error identification for better user feedback");

        // Add top 5 recommendations
        for (int i = 0; i < Math.min(5, topViolations2.size()); i++) {
            String ruleId = topViolations.get(i).getKey();
            String recommendation = recommendationMessages.getOrDefault(ruleId,
                                                                        "Address violations of rule " + ruleId + " which occurred " + topViolations2.get(i).getValue() + " times");
            html.append("        <li>").append(recommendation).append("</li>\n");
        }

        html.append("    </ol>\n");

        html.append("</body>\n");
        html.append("</html>");

        Files.writeString(Paths.get(reportPath), html.toString());
        LOGGER.info("Generated summary report: " + reportPath);
    }

    /**
     * Create output directory if it doesn't exist
     * @param outputDir Output directory path
     */
    private void createOutputDirectory(String outputDir) throws IOException {
        Path outputPath = Paths.get(outputDir);
        if (!Files.exists(outputPath)) {
            Files.createDirectories(outputPath);
        }
    }

    /**
     * Get file name from URL
     * @param url URL string
     * @return File name derived from URL
     */
    private String getFileNameFromUrl(String url) {
        // Remove protocol and host
        String path = url.replaceFirst("https?://", "");

        // Remove query parameters and fragment
        int queryIndex = path.indexOf('?');
        if (queryIndex > 0) {
            path = path.substring(0, queryIndex);
        }

        int fragmentIndex = path.indexOf('#');
        if (fragmentIndex > 0) {
            path = path.substring(0, fragmentIndex);
        }

        // Replace invalid file name characters
        path = path.replaceAll("[^a-zA-Z0-9.-]", "_");

        // Add HTML extension if no extension
        if (!path.contains(".")) {
            path += ".html";
        }

        return path;
    }

    /**
     * Print summary of result for a URL
     * @param result Remediation result
     * @param outputFilePath Path to the fixed file
     */
    private void printResultSummary(RemediationResult result, String outputFilePath) {
        System.out.println("\n=== WCAG Compliance Results ===");
        System.out.println("URL: " + result.getPageUrl());
        System.out.println("Total violations: " + result.getViolations().size());
        System.out.println("Fixes applied: " + result.getFixesApplied());

        if (result.getViolations().size() > 0) {
            double compliancePercentage = (double) result.getFixesApplied() / result.getViolations().size() * 100;
            System.out.println("Automated compliance improvement: " + String.format("%.1f", compliancePercentage) + "%");
        }

        if (outputFilePath != null) {
            System.out.println("Fixed HTML saved to: " + outputFilePath);
        }

        if (!result.getViolations().isEmpty()) {
            System.out.println("\nTop violations by severity:");

            // Group violations by severity
            Map<String, List<Violation>> violationsBySeverity = result.getViolations().stream()
                    .collect(Collectors.groupingBy(Violation::getSeverity));

            // Print critical violations first
            printViolationsForSeverity(violationsBySeverity, "critical");
            printViolationsForSeverity(violationsBySeverity, "serious");
            printViolationsForSeverity(violationsBySeverity, "moderate");
            printViolationsForSeverity(violationsBySeverity, "minor");
        }
    }

    /**
     * Print violations for a specific severity
     * @param violationsBySeverity Map of severity to violations
     * @param severity Severity to print
     */
    private void printViolationsForSeverity(Map<String, List<Violation>> violationsBySeverity, String severity) {
        List<Violation> violations = violationsBySeverity.getOrDefault(severity, Collections.emptyList());
        if (!violations.isEmpty()) {
            System.out.println("\n" + severity.toUpperCase() + " (" + violations.size() + "):");

            // Group by rule ID
            Map<String, Long> violationCounts = violations.stream()
                    .collect(Collectors.groupingBy(Violation::getRuleId, Collectors.counting()));

            // Print top 3 violation types
            violationCounts.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .limit(3)
                    .forEach(entry -> {
                        String ruleId = entry.getKey();
                        String message = violations.stream()
                                .filter(v -> v.getRuleId().equals(ruleId))
                                .findFirst()
                                .map(Violation::getMessage)
                                .orElse("");

                        System.out.println("  - " + message + " (" + entry.getValue() + " occurrences)");
                    });

            // If there are more than 3 types, add a summary line
            if (violationCounts.size() > 3) {
                System.out.println("  - And " + (violationCounts.size() - 3) + " more violation types...");
            }
        }
    }

    /**
     * Print summary for a single file
     * @param filePath File path
     * @param result Remediation result
     */
    private void printFileResultSummary(String filePath, RemediationResult result) {
        System.out.println("\nFile: " + filePath);
        System.out.println("Violations: " + result.getViolations().size() + ", Fixes: " + result.getFixesApplied());

        if (result.getViolations().size() > 0) {
            double compliancePercentage = (double) result.getFixesApplied() / result.getViolations().size() * 100;
            System.out.println("Improvement: " + String.format("%.1f", compliancePercentage) + "%");
        }
    }

    /**
     * Capitalize the first letter of a string
     * @param str String to capitalize
     * @return Capitalized string
     */
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    /**
     * Escape HTML special characters
     * @param input Input string
     * @return Escaped HTML string
     */
    private String escapeHtml(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}