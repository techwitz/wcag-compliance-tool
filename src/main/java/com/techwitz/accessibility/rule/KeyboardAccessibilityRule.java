package com.techwitz.accessibility.rule;

import com.techwitz.accessibility.Violation;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

/**
 * Rule for keyboard accessibility
 */
public class KeyboardAccessibilityRule implements WcagRule {
    @Override
    public String getId() {
        return "2.1.1-keyboard-access";
    }

    @Override
    public String getDescription() {
        return "All functionality must be available using the keyboard";
    }

    @Override
    public String getSuccessCriterion() {
        return "2.1.1 Keyboard";
    }

    @Override
    public String getLevel() {
        return "A";
    }

    @Override
    public List<Violation> evaluate(Document document) {
        List<Violation> violations = new ArrayList<>();

        // Check for elements with mouse event handlers but no keyboard event handlers
        Elements clickableElements = document.select("[onclick], [onmousedown], [onmouseup], [onmouseover], [ondblclick]");
        for (Element element : clickableElements) {
            // Skip if it's a naturally keyboard-accessible element
            if (isNaturallyKeyboardAccessible(element)) {
                continue;
            }

            // Skip if it has appropriate keyboard support
            if (hasKeyboardSupport(element)) {
                continue;
            }

            violations.add(new Violation.Builder()
                                   .setRuleId(getId())
                                   .setMessage("Element has mouse event handlers but may not be keyboard accessible")
                                   .setElement(element.outerHtml())
                                   .setXpath(getXPath(element))
                                   .setSeverity("critical")
                                   .setAutoFixable(true)
                                   .setRemediation("Add keyboard event handlers (onkeypress/onkeydown) or convert to a native button/link")
                                   .build());
        }

        // Check for tabindex with positive values (disrupts natural tab order)
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
                                           .setSeverity("serious")
                                           .setAutoFixable(true)
                                           .setRemediation("Use tabindex='0' for elements that should be in the natural tab order")
                                           .build());
                }
            } catch (NumberFormatException e) {
                // Ignore if tabindex isn't a valid number
            }
        }

        // Check for native interactive elements with negative tabindex
        Elements interactiveElements = document.select("a[href], button, input:not([type=hidden]), select, textarea, [role=button], [role=link]");
        for (Element element : interactiveElements) {
            if (element.hasAttr("tabindex")) {
                try {
                    int tabIndex = Integer.parseInt(element.attr("tabindex"));
                    if (tabIndex < 0) {
                        violations.add(new Violation.Builder()
                                               .setRuleId(getId())
                                               .setMessage("Interactive element with negative tabindex is not keyboard accessible")
                                               .setElement(element.outerHtml())
                                               .setXpath(getXPath(element))
                                               .setSeverity("critical")
                                               .setAutoFixable(true)
                                               .setRemediation("Remove negative tabindex or change to tabindex='0'")
                                               .build());
                    }
                } catch (NumberFormatException e) {
                    // Ignore if tabindex isn't a valid number
                }
            }
        }

        // Check for drag-and-drop without keyboard alternative
        Elements draggableElements = document.select("[draggable=true], [ondrag], [ondragstart], [ondrop]");
        for (Element element : draggableElements) {
            // Look for evidence of keyboard alternative (oversimplified check)
            if (!element.outerHtml().contains("keyboard") && !hasKeyboardSupport(element)) {
                violations.add(new Violation.Builder()
                                       .setRuleId(getId())
                                       .setMessage("Draggable element may not have keyboard alternative")
                                       .setElement(element.outerHtml())
                                       .setXpath(getXPath(element))
                                       .setSeverity("serious")
                                       .setAutoFixable(false)
                                       .setRemediation("Provide keyboard alternative for drag-and-drop functionality")
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
                    if (violation.getMessage().contains("positive tabindex")) {
                        // Fix positive tabindex
                        element.attr("tabindex", "0");
                        fixes++;
                    } else if (violation.getMessage().contains("negative tabindex")) {
                        // Fix negative tabindex on interactive elements
                        element.attr("tabindex", "0");
                        fixes++;
                    } else if (violation.getMessage().contains("not keyboard accessible")) {
                        // Add keyboard event handlers
                        if (element.hasAttr("onclick") && !element.hasAttr("onkeypress") && !element.hasAttr("onkeydown")) {
                            String onclickValue = element.attr("onclick");
                            element.attr("onkeypress", "if(event.key === 'Enter' || event.keyCode === 13) { " + onclickValue + " }");

                            // Add tabindex if not present
                            if (!element.hasAttr("tabindex")) {
                                element.attr("tabindex", "0");
                            }

                            // Add role if appropriate
                            if (!element.hasAttr("role") && element.tagName().equals("div") || element.tagName().equals("span")) {
                                element.attr("role", "button");
                            }

                            fixes++;
                        } else if (element.hasAttr("onmousedown") && !element.hasAttr("onkeydown")) {
                            String onmousedownValue = element.attr("onmousedown");
                            element.attr("onkeydown", "if(event.key === 'Enter' || event.keyCode === 13) { " + onmousedownValue + " }");

                            if (!element.hasAttr("tabindex")) {
                                element.attr("tabindex", "0");
                            }

                            fixes++;
                        }
                    }
                }
            }
        }

        return fixes;
    }

    private boolean isNaturallyKeyboardAccessible(Element element) {
        String tagName = element.tagName().toLowerCase();
        return tagName.equals("a") || tagName.equals("button") || tagName.equals("input") ||
                tagName.equals("select") || tagName.equals("textarea") || tagName.equals("option");
    }

    private boolean hasKeyboardSupport(Element element) {
        return element.hasAttr("onkeypress") || element.hasAttr("onkeydown") || element.hasAttr("onkeyup");
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
        if (xpath.contains("tabindex")) {
            if (xpath.contains("positive tabindex")) {
                return document.select("[tabindex]:not([tabindex='0']):not([tabindex='-1'])");
            } else if (xpath.contains("negative tabindex")) {
                return document.select("a[tabindex^='-'], button[tabindex^='-'], input[tabindex^='-'], select[tabindex^='-'], textarea[tabindex^='-']");
            }
            return document.select("[tabindex]");
        } else {
            return document.select("[onclick]:not(a):not(button):not(input):not(select):not(textarea):not([onkeypress]):not([onkeydown]):not([onkeyup]), [onmousedown]:not([onkeydown])");
        }
    }
}
