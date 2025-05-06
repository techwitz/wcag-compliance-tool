package com.techwitz.accessibility.rule;

import com.techwitz.accessibility.Violation;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Rule for ensuring form controls have proper labels
 */
public class FormLabelRule implements WcagRule {
    @Override
    public String getId() {
        return "1.3.1-form-label";
    }

    @Override
    public String getDescription() {
        return "Form controls must have associated labels";
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

        // Find all form controls that typically need labels
        Elements formControls = document.select(
                "input:not([type=button]):not([type=submit]):not([type=reset]):not([type=hidden])," +
                        "select, textarea"
        );

        for (Element control : formControls) {
            // Skip controls in hidden containers
            if (isInHiddenContainer(control)) {
                continue;
            }

            // Check if control has any form of accessible label
            if (!hasAccessibleLabel(control, document)) {
                Violation.Builder builder = new Violation.Builder()
                        .setRuleId(getId())
                        .setElement(control.outerHtml())
                        .setXpath(getXPath(control))
                        .setSeverity("critical")
                        .setAutoFixable(true);

                // Customize the message based on the control type
                if (control.tagName().equals("input")) {
                    String type = control.attr("type");
                    if (type.isEmpty()) type = "text"; // Default input type

                    builder.setMessage(String.format(
                                    "%s input lacks accessible label",
                                    type.substring(0, 1).toUpperCase() + type.substring(1)))
                            .setRemediation(String.format(
                                    "Add a label element with for=\"%s\", aria-label, or aria-labelledby attribute",
                                    control.attr("id")));
                } else {
                    builder.setMessage(String.format(
                                    "%s element lacks accessible label",
                                    control.tagName().substring(0, 1).toUpperCase() + control.tagName().substring(1)))
                            .setRemediation("Add a label element with for attribute or appropriate ARIA attributes");
                }

                violations.add(builder.build());
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

                for (Element control : elements) {
                    // Don't attempt to fix elements that already have some form of label
                    if (hasAccessibleLabel(control, document)) {
                        continue;
                    }

                    // Ensure the control has an ID
                    String id = control.attr("id");
                    if (id.isEmpty()) {
                        id = "generated-id-" + UUID.randomUUID().toString().substring(0, 8);
                        control.attr("id", id);
                    }

                    // Create a label with appropriate text
                    Element label = document.createElement("label");
                    label.attr("for", id);

                    // Try to determine a reasonable label text
                    String labelText = generateLabelText(control);
                    label.text(labelText);

                    // Insert the label before the control
                    control.before(label);
                    fixes++;
                }
            }
        }

        return fixes;
    }

    private boolean isInHiddenContainer(Element element) {
        // Check if element or any parent has hidden, display:none, or visibility:hidden
        if (element.hasAttr("hidden") ||
                element.hasAttr("style") && element.attr("style").contains("display: none") ||
                element.hasAttr("style") && element.attr("style").contains("visibility: hidden")) {
            return true;
        }

        // Check parent elements
        for (Element parent : element.parents()) {
            if (parent.hasAttr("hidden") ||
                    parent.hasAttr("style") && parent.attr("style").contains("display: none") ||
                    parent.hasAttr("style") && parent.attr("style").contains("visibility: hidden")) {
                return true;
            }
        }

        return false;
    }

    private boolean hasAccessibleLabel(Element control, Document document) {
        // Check for label element
        String id = control.attr("id");
        if (!id.isEmpty()) {
            Elements labels = document.select("label[for=" + id + "]");
            if (!labels.isEmpty()) {
                return true;
            }
        }

        // Check for aria-label attribute
        if (control.hasAttr("aria-label") && !control.attr("aria-label").trim().isEmpty()) {
            return true;
        }

        // Check for aria-labelledby attribute
        if (control.hasAttr("aria-labelledby") && !control.attr("aria-labelledby").trim().isEmpty()) {
            String labelledBy = control.attr("aria-labelledby");
            for (String labelId : labelledBy.split("\\s+")) {
                if (!labelId.trim().isEmpty() && document.getElementById(labelId) != null) {
                    return true;
                }
            }
        }

        // Check for title attribute
        if (control.hasAttr("title") && !control.attr("title").trim().isEmpty()) {
            return true;
        }

        // Check for placeholder (not sufficient for accessibility, but some frameworks use it as a fallback)
        if (control.hasAttr("placeholder") && !control.attr("placeholder").trim().isEmpty()) {
            return true;
        }

        // Check if control is wrapped by a label
        if (control.parent() != null && control.parent().tagName().equals("label")) {
            return true;
        }

        return false;
    }

    private String generateLabelText(Element control) {
        // Try to find a good label from various attributes

        // 1. Check for placeholder text
        if (control.hasAttr("placeholder")) {
            String placeholder = control.attr("placeholder").trim();
            if (!placeholder.isEmpty()) {
                return placeholder;
            }
        }

        // 2. Check for name attribute
        if (control.hasAttr("name")) {
            String name = control.attr("name");
            // Convert snake_case or camelCase to readable text
            name = name.replace("_", " ").replace("-", " ");

            // Add spaces before capital letters in camelCase
            StringBuilder readableName = new StringBuilder();
            for (int i = 0; i < name.length(); i++) {
                char c = name.charAt(i);
                if (i > 0 && Character.isUpperCase(c)) {
                    readableName.append(' ');
                }
                readableName.append(c);
            }

            // Capitalize first letter of each word
            String[] words = readableName.toString().split("\\s+");
            StringBuilder result = new StringBuilder();
            for (String word : words) {
                if (!word.isEmpty()) {
                    result.append(Character.toUpperCase(word.charAt(0)))
                            .append(word.substring(1))
                            .append(" ");
                }
            }

            return result.toString().trim() + ":";
        }

        // 3. Default based on input type
        if (control.tagName().equals("input")) {
            String type = control.attr("type").toLowerCase();
            if (type.isEmpty()) type = "text";

            switch (type) {
                case "text":
                    return "Text:";
                case "email":
                    return "Email:";
                case "password":
                    return "Password:";
                case "tel":
                    return "Phone:";
                case "date":
                    return "Date:";
                case "time":
                    return "Time:";
                case "number":
                    return "Number:";
                case "search":
                    return "Search:";
                case "checkbox":
                    return "Checkbox label";
                case "radio":
                    return "Radio option";
                default:
                    return type.substring(0, 1).toUpperCase() + type.substring(1) + ":";
            }
        } else if (control.tagName().equals("select")) {
            return "Select:";
        } else if (control.tagName().equals("textarea")) {
            return "Comments:";
        }

        // If we can't determine a good label, use a placeholder
        return "[LABEL NEEDED]";
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
        } else if (element.hasAttr("name")) {
            path.append("[@name='").append(element.attr("name")).append("']");
        }

        return path.toString();
    }

    private Elements findElementsByXPath(Document document, String xpath) {
        // Simplified implementation
        if (xpath.contains("/input") || xpath.contains("/select") || xpath.contains("/textarea")) {
            String selector = "input:not([type=button]):not([type=submit]):not([type=reset]):not([type=hidden])," +
                    "select, textarea";

            // Try to find the specific element if we have an ID or name
            if (xpath.contains("[@id='")) {
                int start = xpath.indexOf("[@id='") + 6;
                int end = xpath.indexOf("']", start);
                if (start >= 6 && end > start) {
                    String id = xpath.substring(start, end);
                    return document.select("#" + id);
                }
            } else if (xpath.contains("[@name='")) {
                int start = xpath.indexOf("[@name='") + 8;
                int end = xpath.indexOf("']", start);
                if (start >= 8 && end > start) {
                    String name = xpath.substring(start, end);
                    return document.select("[name=" + name + "]");
                }
            }

            // If we can't find the specific element, return all potentially unlabelled form controls
            return document.select(selector);
        }

        return new Elements();
    }
}
