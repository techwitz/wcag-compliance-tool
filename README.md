# WCAG 2.3 Compliance Tool

A comprehensive Java-based tool for analyzing and remediating WCAG 2.3 accessibility issues in web pages and source code.

## Table of Contents

- [Features](#features)
- [Installation](#installation)
- [Usage](#usage)
    - [Command Line Interface](#command-line-interface)
    - [Programmatic API](#programmatic-api)
- [WCAG Rules Implemented](#wcag-rules-implemented)
    - [Level A Rules](#level-a-rules)
    - [Level AA Rules](#level-aa-rules)
    - [Level AAA Rules](#level-aaa-rules)
- [Reports](#reports)
- [Configuration Options](#configuration-options)
- [Examples](#examples)
- [Requirements](#requirements)
- [Contributing](#contributing)
- [License](#license)

## Features

- **Comprehensive WCAG 2.3 Compliance Checking**: Analyzes HTML for compliance with WCAG 2.3 accessibility standards
- **Automatic Remediation**: Applies fixes to common accessibility issues where possible
- **Detailed Reports**: Generates detailed reports with specific remediation guidance
- **Multiple Input Options**:
    - Process individual URLs
    - Analyze entire source code folders
    - Support for HTML, JSP, JavaScript, and other web files
- **Non-disruptive**: Preserves original content while enhancing accessibility
- **Flexible Configuration**: Customize rules, remediation options, and output formats
- **Parallel Processing**: Efficiently handle multiple files simultaneously

## Installation

### Option 1: Build from source

```bash
# Clone the repository
git clone https://github.com/techwitz/wcag-compliance-tool.git
cd wcag-compliance-tool

# Build with Maven
mvn clean package

# Or build with Gradle
./gradlew build
```

### Option 2: Download the JAR

Download the pre-built JAR file from the [releases page](https://github.com/your-org/wcag-compliance-tool/releases).

## Usage

### Command Line Interface

The tool provides a command-line interface for easy integration into your workflows:

#### Analyze a URL

```bash
java -jar wcag-tool.jar --url https://example.com --output-dir ./reports
```

#### Apply fixes to a URL

```bash
java -jar wcag-tool.jar --url https://example.com --output-dir ./reports --apply-fixes
```

#### Analyze a source code folder

```bash
java -jar wcag-tool.jar --source-dir ./my-project --output-dir ./reports
```

#### Apply fixes to source code

```bash
java -jar wcag-tool.jar --source-dir ./my-project --output-dir ./reports --apply-fixes
```

#### Include specific file patterns

```bash
java -jar wcag-tool.jar --source-dir ./my-project --include "**/*.html" --include "**/*.jsp"
```

#### Exclude specific file patterns

```bash
java -jar wcag-tool.jar --source-dir ./my-project --exclude "**/vendor/**" --exclude "**/node_modules/**"
```

#### Include AA and AAA level checks

```bash
java -jar wcag-tool.jar --source-dir ./my-project --include-aa --include-aaa
```

### Programmatic API

You can also use the tool programmatically in your Java applications:

```java
// Create configuration
ComplianceConfig config = new ComplianceConfig.Builder()
    .setIncludeLevel2Rules(true)
    .setIncludeLevel3Rules(false)
    .build();

// Create tool instance
WcagComplianceTool tool = new WcagComplianceTool(config);

// Analyze a URL or file
ComplianceReport report = tool.analyzePage("https://example.com");

// Apply fixes
RemediationOptions options = new RemediationOptions.Builder()
    .setAutoFix(true)
    .setApplyAriaEnhancements(true)
    .setFixContrastIssues(true)
    .setSaveToPath("fixed.html")
    .build();

RemediationResult result = tool.remediatePage("https://example.com", options);
```

## WCAG Rules Implemented

The tool implements a comprehensive set of rules based on the WCAG 2.3 specifications, including all the latest additions from WCAG 2.2.

### Level A Rules

#### Perceivable Information

- **1.1.1 Non-text Content (ImageAltTextRule)**
    - Ensures images have appropriate alt text
    - Detects missing alt attributes, empty alt text, and generic alt text
    - Provides automatic remediation for missing alt attributes

- **1.3.1 Info and Relationships**
    - **FormLabelRule**: Ensures form controls have proper labels
    - **HeadingStructureRule**: Checks for proper heading hierarchy
    - **TableHeadersRule**: Ensures data tables have appropriate headers

#### Operable User Interface

- **2.1.1 Keyboard (KeyboardAccessibilityRule)**
    - Ensures all functionality is available via keyboard
    - Checks for mouse-dependent events without keyboard alternatives
    - Adds keyboard event handlers and focus management

- **2.4.4 Link Purpose (LinkPurposeRule)**
    - Validates link text clarity
    - Detects generic link text like "click here"
    - Identifies links with URLs as text

#### Understandable Information

- **3.1.1 Language of Page (LanguageAttributeRule)**
    - Checks for proper language attributes
    - Ensures the main document has a lang attribute
    - Detects content in different languages

- **3.2.6 Consistent Help (ConsistentHelpRule)** - WCAG 2.2
    - Ensures help mechanisms appear in consistent locations
    - Analyzes the position of help links, FAQs, and contact information
    - Verifies consistency across pages

- **3.3.1 Error Identification (ErrorIdentificationRule)**
    - Ensures form errors are properly identified
    - Checks for appropriate ARIA attributes on error messages
    - Ensures required fields are properly marked

### Level AA Rules

#### Perceivable Information

- **1.4.3 Contrast (ColorContrastRule)**
    - Ensures text has sufficient contrast with its background
    - Checks for contrast ratios based on text size
    - Implements automatic contrast fixes

#### Operable User Interface

- **2.4.11 Focus Not Obscured (FocusNotObscuredRule)** - WCAG 2.2
    - Ensures focused elements aren't hidden by overlays
    - Checks sticky headers/footers that might obscure focus
    - Automatically adjusts z-index values and scrolling behavior

- **2.5.7 Dragging Movements (DraggingMovementsRule)** - WCAG 2.2
    - Ensures dragging operations have non-dragging alternatives
    - Identifies sliders, sortable lists, and drag-drop interfaces
    - Adds button alternatives and keyboard controls

#### Understandable Information

- **3.3.8 Accessible Authentication (AccessibleAuthenticationRule)** - WCAG 2.2
    - Ensures authentication processes don't rely solely on cognitive function tests
    - Checks for CAPTCHA, security questions, and verification codes
    - Implements "show password" functionality and ensures copy-paste is enabled

### Level AAA Rules

The tool can be configured to include Level AAA checks when the `includeLevel3Rules` option is enabled. These rules represent the highest level of accessibility compliance.

## Reports

The tool generates two types of reports:

1. **Individual Page Reports**: Detailed analysis of each page with specific violations and remediation guidance:
    - Violations categorized by severity (critical, serious, moderate, minor)
    - Code snippets showing problematic elements
    - Specific recommendations for fixing each issue
    - Before/after HTML comparison when fixes are applied
    - Visual indicators showing which issues were automatically fixed

2. **Summary Reports**: Overview of all analyzed pages with aggregated statistics:
    - Total violations found and fixed
    - Violations by WCAG level and rule
    - Compliance improvement metrics
    - Charts visualizing most common issues
    - Top recommended focus areas for further improvement

## Configuration Options

The tool provides various configuration options through the `ComplianceConfig` class:

- **Rule Selection**: Choose which WCAG levels to include (A, AA, AAA)
- **Custom Rules**: Add your own rule implementations
- **CSS Properties**: Customize visual enhancements for contrast issues
- **Exclusion Patterns**: Exclude specific files or patterns from analysis

The `RemediationOptions` class allows control over how fixes are applied:

- **Auto Fix**: Enable/disable automatic fixes
- **Preserve Comments**: Keep HTML comments during remediation
- **Apply ARIA Enhancements**: Add ARIA attributes to improve accessibility
- **Fix Contrast Issues**: Apply contrast fixes to improve readability

## Examples

### Example 1: Quick URL Analysis

```java
public class QuickUrlAnalysis {
    public static void main(String[] args) throws IOException {
        // Create a tool with default configuration
        WcagComplianceTool tool = new WcagComplianceTool(new ComplianceConfig.Builder().build());
        
        // Analyze a URL
        ComplianceReport report = tool.analyzePage("https://example.com");
        
        // Print summary of violations
        System.out.println("Total violations: " + report.getTotalViolations());
        System.out.println("Auto-fixable violations: " + report.getAutoFixableViolations());
        
        // Print violations by level
        Map<String, Integer> byLevel = report.getViolationsByLevel();
        for (Map.Entry<String, Integer> entry : byLevel.entrySet()) {
            System.out.println("Level " + entry.getKey() + ": " + entry.getValue());
        }
    }
}
```

### Example 2: Processing Multiple Files

```java
public class BatchProcessingExample {
    public static void main(String[] args) throws IOException {
        // Configure the tool
        ComplianceConfig config = new ComplianceConfig.Builder()
            .setIncludeLevel2Rules(true)
            .setCssProperty(".text-light", "color: #000000;")
            .build();
        
        WcagComplianceTool tool = new WcagComplianceTool(config);
        
        // Setup remediation options
        RemediationOptions options = new RemediationOptions.Builder()
            .setAutoFix(true)
            .setApplyAriaEnhancements(true)
            .build();
        
        // Process multiple files
        List<String> filePaths = Arrays.asList(
            "src/main/webapp/index.html",
            "src/main/webapp/about.html",
            "src/main/webapp/contact.jsp"
        );
        
        Map<String, RemediationResult> results = tool.batchProcess(filePaths, options);
        
        // Print results
        for (Map.Entry<String, RemediationResult> entry : results.entrySet()) {
            System.out.println("File: " + entry.getKey());
            System.out.println("Violations: " + entry.getValue().getViolations().size());
            System.out.println("Fixes applied: " + entry.getValue().getFixesApplied());
        }
    }
}
```

## Requirements

- Java 21 or higher
- Gradle 8.5 or higher
- JSoup 1.15.3 or higher (for HTML parsing)
- Commons-Lang3 3.12.0 or higher (for utility functions)
- SLF4J 2.0.5 or higher (for logging)

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/my-new-rule`)
3. Implement your changes
4. Add tests for your changes
5. Commit your changes (`git commit -m 'Add new rule for focus appearance'`)
6. Push to the branch (`git push origin feature/my-new-rule`)
7. Create a new Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.