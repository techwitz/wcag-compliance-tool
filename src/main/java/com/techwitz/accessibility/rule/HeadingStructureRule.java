package com.techwitz.accessibility.rule;

import com.techwitz.accessibility.Violation;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

/**
 * Rule for checking heading structure
 */
public class HeadingStructureRule implements WcagRule {
    @Override
    public String getId() {
        return "1.3.1-heading-structure";
    }

    @Override
    public String getDescription() {
        return "Heading levels should be used in correct order";
    }

    @Override
    public String getSuccessCriterion() {
        return "1.3.1 Info and Relationships";
    }

    @Override
    public String getLevel() {
        return "A";
    }

    @Override
    public List<Violation> evaluate(Document document) {
        List<Violation> violations = new ArrayList<>();

        // Get all headings
        Elements headings = document.select("h1, h2, h3, h4, h5, h6");

        if (headings.isEmpty()) {
            // No headings found - flag as an issue for content structure
            violations.add(new Violation.Builder()
                                   .setRuleId(getId())
                                   .setMessage("Page has no heading elements (h1-h6)")
                                   .setElement("<body>" + (document.body() != null ? "..." : "") + "</body>")
                                   .setXpath("/html/body")
                                   .setSeverity("serious")
                                   .setAutoFixable(false)
                                   .setRemediation("Add appropriate heading elements to structure content")
                                   .build());
            return violations;
        }

        // Check for H1
        Elements h1Elements = document.select("h1");
        if (h1Elements.isEmpty()) {
            violations.add(new Violation.Builder()
                                   .setRuleId(getId())
                                   .setMessage("Page does not contain an H1 heading")
                                   .setElement("<body>" + (document.body() != null ? "..." : "") + "</body>")
                                   .setXpath("/html/body")
                                   .setSeverity("serious")
                                   .setAutoFixable(false)
                                   .setRemediation("Add an H1 heading that describes the main content of the page")
                                   .build());
        } else if (h1Elements.size() > 1) {
            // Multiple H1s - flag all after the first one
            for (int i = 1; i < h1Elements.size(); i++) {
                Element h1 = h1Elements.get(i);
                violations.add(new Violation.Builder()
                                       .setRuleId(getId())
                                       .setMessage("Multiple H1 headings found - page should generally have only one H1")
                                       .setElement(h1.outerHtml())
                                       .setXpath(getXPath(h1))
                                       .setSeverity("moderate")
                                       .setAutoFixable(true)
                                       .setRemediation("Change additional H1 elements to H2 or lower heading levels")
                                       .build());
            }
        }

        // Check for skipped heading levels
        int previousLevel = 0;

        for (Element heading : headings) {
            int currentLevel = Integer.parseInt(heading.tagName().substring(1));

            // First heading should be H1
            if (previousLevel == 0 && currentLevel > 1) {
                violations.add(new Violation.Builder()
                                       .setRuleId(getId())
                                       .setMessage("First heading on page is H" + currentLevel + ", should be H1")
                                       .setElement(heading.outerHtml())
                                       .setXpath(getXPath(heading))
                                       .setSeverity("moderate")
                                       .setAutoFixable(true)
                                       .setRemediation("Change to H1 or add an H1 before this heading")
                                       .build());
            }
            // Check for skipped levels (e.g., H1 to H3)
            else if (previousLevel > 0 && currentLevel > previousLevel + 1) {
                violations.add(new Violation.Builder()
                                       .setRuleId(getId())
                                       .setMessage("Heading level skipped from H" + previousLevel + " to H" + currentLevel)
                                       .setElement(heading.outerHtml())
                                       .setXpath(getXPath(heading))
                                       .setSeverity("moderate")
                                       .setAutoFixable(true)
                                       .setRemediation("Use sequential heading levels (H" + (previousLevel + 1) + " instead of H" + currentLevel + ")")
                                       .build());
            }

            previousLevel = currentLevel;
        }

        // Check for empty headings
        for (Element heading : headings) {
            if (heading.text().trim().isEmpty()) {
                violations.add(new Violation.Builder()
                                       .setRuleId(getId())
                                       .setMessage("Empty heading element found")
                                       .setElement(heading.outerHtml())
                                       .setXpath(getXPath(heading))
                                       .setSeverity("serious")
                                       .setAutoFixable(false)
                                       .setRemediation("Add descriptive text to the heading or remove it")
                                       .build());
            }
        }

        return violations;
    }

    @Override
    public boolean canAutoFix() {
        return true;
    }

    @Override
    public int applyFixes(Document document, List<Violation> violations) {
        int fixes = 0;

        for (Violation violation : violations) {
            if (violation.getRuleId().equals(getId()) && violation.isAutoFixable()) {
                String xpath = violation.getXpath();
                Elements elements = findElementsByXPath(document, xpath);

                for (Element element : elements) {
                    String tagName = element.tagName();

                    if (tagName.matches("h[1-6]")) {
                        String message = violation.getMessage();

                        if (message.contains("Multiple H1 headings found")) {
                            // Change extra H1s to H2
                            element.tagName("h2");
                            fixes++;
                        } else if (message.contains("Heading level skipped")) {
                            // Extract the recommended level from the remediation
                            String remediation = violation.getRemediation();
                            if (remediation.contains("H")) {
                                int startIdx = remediation.indexOf("H") + 1;
                                int endIdx = remediation.indexOf(" ", startIdx);
                                if (endIdx == -1) endIdx = remediation.length();

                                try {
                                    int suggestedLevel = Integer.parseInt(remediation.substring(startIdx, endIdx));
                                    element.tagName("h" + suggestedLevel);
                                    fixes++;
                                } catch (NumberFormatException e) {
                                    // Skip if parsing fails
                                }
                            }
                        } else if (message.contains("First heading on page is H")) {
                            // Change to H1
                            element.tagName("h1");
                            fixes++;
                        }
                    }
                }
            }
        }

        return fixes;
    }

    private String getXPath(Element element) {
        StringBuilder path = new StringBuilder();
        Elements ancestors = element.parents();

        for (int i = ancestors.size() - 1; i >= 0; i--) {
            Element ancestor = ancestors.get(i);
            path.append("/").append(ancestor.tagName());

            if (ancestor.hasAttr("id")) {
                path.append("[@id='").append(ancestor.attr("id")).append("']");
            }
        }

        path.append("/").append(element.tagName());

        if (element.hasAttr("id")) {
            path.append("[@id='").append(element.attr("id")).append("']");
        }

        return path.toString();
    }

    private Elements findElementsByXPath(Document document, String xpath) {
        // Simplified implementation
        if (xpath.contains("/h1") || xpath.contains("/h2") || xpath.contains("/h3") ||
                xpath.contains("/h4") || xpath.contains("/h5") || xpath.contains("/h6")) {

            // Try to find a specific heading by ID
            if (xpath.contains("[@id='")) {
                int start = xpath.indexOf("[@id='") + 6;
                int end = xpath.indexOf("']", start);
                if (start >= 6 && end > start) {
                    String id = xpath.substring(start, end);
                    return document.select("h1#" + id + ", h2#" + id + ", h3#" + id +
                                                   ", h4#" + id + ", h5#" + id + ", h6#" + id);
                }
            }

            // If checking for additional H1s
            if (xpath.contains("/h1") && xpath.indexOf("/h1") == xpath.length() - 3) {
                return document.select("h1:gt(0)"); // All H1s after the first one
            }

            // For skipped levels or generic heading issues, extract the heading tag
            String tag = xpath.substring(xpath.lastIndexOf("/") + 1);
            if (tag.matches("h[1-6]")) {
                return document.select(tag);
            }
        }

        return new Elements();
    }
}
