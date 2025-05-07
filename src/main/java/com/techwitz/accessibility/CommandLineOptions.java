package com.techwitz.accessibility;

import java.util.ArrayList;
import java.util.List;

/**
 * Command-line options for the application
 */
class CommandLineOptions {
    private String url;
    private String sourceDir;
    private String outputDir = "./wcag-reports";
    private boolean applyFixes = false;
    private boolean includeAA = true;
    private boolean includeAAA = false;
    private List<String> includePatterns = new ArrayList<>();
    private List<String> excludePatterns = new ArrayList<>();

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getSourceDir() {
        return sourceDir;
    }

    public void setSourceDir(String sourceDir) {
        this.sourceDir = sourceDir;
    }

    public String getOutputDir() {
        return outputDir;
    }

    public void setOutputDir(String outputDir) {
        this.outputDir = outputDir;
    }

    public boolean isApplyFixes() {
        return applyFixes;
    }

    public void setApplyFixes(boolean applyFixes) {
        this.applyFixes = applyFixes;
    }

    public boolean isIncludeAA() {
        return includeAA;
    }

    public void setIncludeAA(boolean includeAA) {
        this.includeAA = includeAA;
    }

    public boolean isIncludeAAA() {
        return includeAAA;
    }

    public void setIncludeAAA(boolean includeAAA) {
        this.includeAAA = includeAAA;
    }

    public List<String> getIncludePatterns() {
        return includePatterns;
    }

    public void addIncludePattern(String pattern) {
        this.includePatterns.add(pattern);
    }

    public List<String> getExcludePatterns() {
        return excludePatterns;
    }

    public void addExcludePattern(String pattern) {
        this.excludePatterns.add(pattern);
    }
}