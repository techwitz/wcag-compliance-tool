# WCAG 2.3 Compliance Tool

A comprehensive Java-based tool for analyzing and remediating WCAG 2.3 accessibility issues in web pages and source code.

## Features

- **Comprehensive WCAG 2.3 Compliance Checking**: Analyzes HTML for compliance with WCAG 2.3 accessibility standards
- **Automatic Remediation**: Applies fixes to common accessibility issues where possible
- **Detailed Reports**: Generates detailed reports with specific remediation guidance
- **Multiple Input Options**: Process individual URLs or entire source code folders
- **Non-disruptive**: Preserves original content while enhancing accessibility
- **Flexible Configuration**: Customize rules, remediation options, and output formats

## Prerequisites

- Java 11 or higher
- Maven or Gradle for building

## Installation

### Option 1: Build from source

```bash
# Clone the repository
git clone https://github.com/your-org/wcag-compliance-tool.git
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

## Reports

The tool generates two types of reports:

1. **Individual Page Reports**: Detailed analysis of each page with specific violations and remediation guidance
2. **Summary Reports**: Overview of all analyzed pages with aggregated statistics

Reports are generated in HTML format and include:

- Total violations found and fixed
- Violations by WCAG level and rule
- Specific elements with issues
- Recommended fixes for each issue
- Before/after code comparisons

## Supported WCAG 2.3 Rules

The tool checks for compliance with the following WCAG 2.3 guidelines:

### Level A (Essential)

- **1.1.1 Non-text Content**: Ensures images have alt text
- **1.3.1 Info and Relationships**: Checks form labels, heading structure, and table headers
- **2.1.1 Keyboard**: Verifies keyboard accessibility
- **2.4.4 Link Purpose**: Validates link text clarity
- **3.1.1 Language of Page**: Checks for language attributes
- **3.3.1 Error Identification**: Ensures form errors are properly identified

### Level AA (Enhanced)

- **1.4.3 Contrast**: Validates color contrast ratios
- **2.4.6 Headings and Labels**: Ensures descriptive headings and labels
- **3.2.4 Consistent Identification**: Checks for consistent component naming

## Configuration Options

The tool provides various configuration options:

- **Rule Selection**: Choose which WCAG levels to include (A, AA, AAA)
- **Remediation Options**: Control which automatic fixes to apply
- **CSS Properties**: Customize visual enhancements for contrast issues
- **Exclusion Patterns**: Exclude specific files or patterns from analysis

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.