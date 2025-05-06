package com.techwitz.accessibility.rule;

import com.techwitz.accessibility.Violation;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

public class FocusOrderRule implements WcagRule {
    @Override
    public String getId() {
        return "2.4.3-focus-order";
    }

    @Override
    public String getDescription() {
        return "Focus order must be logical and intuitive";
    }

    @Override
    public String getSuccessCriterion() {
        return "2.4.3 Focus Order";
    }

    @Override
    public String getLevel() {
        return "A";
    }

    @Override
    public List<Violation> evaluate(Document document) {
        List<Violation> violations = new ArrayList<>();

        // Check for tabindex values that are positive (which disrupts natural tab order)
        Elements positiveTabIndex = document.select("[tabindex]");
        for (Element element : positiveTabIndex) {
            try {
                int tabIndex = Integer.parseInt(element.attr("tabindex"));
                if (tabIndex > 0) {
                    violations.add(new Violation.Builder()
                                           .setRuleId(getId())
                                           .setMessage("Element has positive tabindex (" + tabIndex + ") which disrupts natural tab order")
                                           .setElement(element.outerHtml())
                                           .setXpath(getXPath(element))
                                           .setAutoFixable(true)
                                           .setRemediation("Use tabindex='0' to include in natural focus order or tabindex='-1' to exclude")
                                           .build());
                }
            } catch (NumberFormatException e) {
                // Ignore if tabindex isn't a valid number
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
                    // Convert positive tabindex to 0
                    element.attr("tabindex", "0");
                    fixes++;
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
        return document.select("[tabindex]:not([tabindex='0']):not([tabindex='-1'])");
    }
}
