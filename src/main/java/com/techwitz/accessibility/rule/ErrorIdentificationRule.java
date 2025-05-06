package com.techwitz.accessibility.rule;

import com.techwitz.accessibility.Violation;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

/**
 * Rule for error identification
 */
public class ErrorIdentificationRule implements WcagRule {
    @Override
    public String getId() {
        return "3.3.1-error-identification";
    }

    @Override
    public String getDescription() {
        return "Form errors must be identified programmatically";
    }

    @Override
    public String getSuccessCriterion() {
        return "3.3.1 Error Identification";
    }

    @Override
    public String getLevel() {
        return "A";
    }

    @Override
    public List<Violation> evaluate(Document document) {
        List<Violation> violations = new ArrayList<>();

        // Check for error message containers without appropriate ARIA attributes
        Elements errorContainers = document.select(".error, .error-message, .field-error, " +
                                                           "[id*=error], [class*=error], [id*=invalid], [class*=invalid], " +
                                                           ".validation-message, .alert-danger, .warning");

        for (Element container : errorContainers) {
            // Skip if it already has appropriate error attributes
            if (hasAriaErrorAttributes(container)) {
                continue;
            }

            violations.add(new Violation.Builder()
                                   .setRuleId(getId())
                                   .setMessage("Error message container without appropriate ARIA attributes")
                                   .setElement(container.outerHtml())
                                   .setXpath(getXPath(container))
                                   .setSeverity("serious")
                                   .setAutoFixable(true)
                                   .setRemediation("Add role='alert' or aria-live='assertive' to error message containers")
                                   .build());
        }

        // Check for required form fields without aria-required
        Elements requiredFields = document.select("input[required], select[required], textarea[required]");
        for (Element field : requiredFields) {
            if (!field.hasAttr("aria-required")) {
                violations.add(new Violation.Builder()
                                       .setRuleId(getId())
                                       .setMessage("Required form field without aria-required attribute")
                                       .setElement(field.outerHtml())
                                       .setXpath(getXPath(field))
                                       .setSeverity("moderate")
                                       .setAutoFixable(true)
                                       .setRemediation("Add aria-required='true' to required form fields")
                                       .build());
            }
        }

        // Check for form fields that are likely to display errors
        Elements formFields = document.select("input:not([type=button]):not([type=submit]):not([type=reset]):not([type=hidden]), select, textarea");
        for (Element field : formFields) {
            // Check if field has aria-describedby but target doesn't exist or is hidden
            if (field.hasAttr("aria-describedby")) {
                String[] ids = field.attr("aria-describedby").split("\\s+");
                for (String id : ids) {
                    Element target = document.getElementById(id);
                    if (target == null) {
                        violations.add(new Violation.Builder()
                                               .setRuleId(getId())
                                               .setMessage("Form field references non-existent element in aria-describedby")
                                               .setElement(field.outerHtml())
                                               .setXpath(getXPath(field))
                                               .setSeverity("serious")
                                               .setAutoFixable(false)
                                               .setRemediation("Ensure the ID referenced in aria-describedby exists")
                                               .build());
                    }
                }
            }

            // Check if field has aria-invalid but no explanation
            if (field.hasAttr("aria-invalid") && field.attr("aria-invalid").equals("true") &&
                    !field.hasAttr("aria-describedby") && !field.hasAttr("aria-errormessage")) {

                violations.add(new Violation.Builder()
                                       .setRuleId(getId())
                                       .setMessage("Form field marked as invalid without associated error message")
                                       .setElement(field.outerHtml())
                                       .setXpath(getXPath(field))
                                       .setSeverity("serious")
                                       .setAutoFixable(false)
                                       .setRemediation("Add aria-describedby or aria-errormessage pointing to error explanation")
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
                    if (violation.getMessage().contains("Error message container")) {
                        element.attr("role", "alert");
                        element.attr("aria-live", "assertive");
                        fixes++;
                    } else if (violation.getMessage().contains("Required form field")) {
                        element.attr("aria-required", "true");
                        fixes++;
                    }
                }
            }
        }

        return fixes;
    }

    private boolean hasAriaErrorAttributes(Element element) {
        return (element.hasAttr("role") &&
                (element.attr("role").equals("alert") || element.attr("role").equals("status"))) ||
                (element.hasAttr("aria-live") &&
                        (element.attr("aria-live").equals("assertive") || element.attr("aria-live").equals("polite")));
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
        if (xpath.contains("error") || xpath.contains("Error")) {
            return document.select(".error, .error-message, .field-error, " +
                                           "[id*=error], [class*=error], [id*=invalid], [class*=invalid], " +
                                           ".validation-message, .alert-danger, .warning");
        } else {
            return document.select("input[required], select[required], textarea[required]");
        }
    }
}
