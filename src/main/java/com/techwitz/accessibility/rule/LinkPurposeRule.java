package com.techwitz.accessibility.rule;

import com.techwitz.accessibility.Violation;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;

/**
 * Rule for checking link purpose
 */
public class LinkPurposeRule implements WcagRule {
    @Override
    public String getId() {
        return "2.4.4-link-purpose";
    }

    @Override
    public String getDescription() {
        return "Link purpose must be clear from the link text";
    }

    @Override
    public String getSuccessCriterion() {
        return "2.4.4 Link Purpose (In Context)";
    }

    @Override
    public String getLevel() {
        return "A";
    }

    @Override
    public List<Violation> evaluate(Document document) {
        List<Violation> violations = new ArrayList<>();

        Elements links = document.select("a[href]");
        for (Element link : links) {
            // Skip links that are hidden or in hidden containers
            if (isHidden(link)) {
                continue;
            }

            String linkText = link.text().trim();
            String ariaLabel = link.attr("aria-label").trim();
            String title = link.attr("title").trim();

            // Check for empty links
            if (linkText.isEmpty() && ariaLabel.isEmpty() && title.isEmpty()) {
                // Check if link has image with alt text
                Elements imgElements = link.select("img");
                if (imgElements.isEmpty() || !imgElements.stream()
                        .anyMatch(img -> img.hasAttr("alt") && !img.attr("alt").trim().isEmpty())) {

                    violations.add(new Violation.Builder()
                                           .setRuleId(getId())
                                           .setMessage("Link has no discernible text or accessible name")
                                           .setElement(link.outerHtml())
                                           .setXpath(getXPath(link))
                                           .setSeverity("critical")
                                           .setAutoFixable(false)
                                           .setRemediation("Add descriptive text to the link or provide aria-label")
                                           .build());
                }
            }

            // Check for generic link text
            String accessibleName = !ariaLabel.isEmpty() ? ariaLabel : (!title.isEmpty() ? title : linkText);
            String accessibleNameLower = accessibleName.toLowerCase();

            if (!accessibleName.isEmpty() && isGenericLinkText(accessibleNameLower)) {
                violations.add(new Violation.Builder()
                                       .setRuleId(getId())
                                       .setMessage("Link text is too generic: \"" + accessibleName + "\"")
                                       .setElement(link.outerHtml())
                                       .setXpath(getXPath(link))
                                       .setSeverity("moderate")
                                       .setAutoFixable(false)
                                       .setRemediation("Replace with descriptive text that explains the link's specific purpose")
                                       .build());
            }

            // Check for URLs as link text
            if (!accessibleName.isEmpty() &&
                    (accessibleNameLower.startsWith("http://") ||
                            accessibleNameLower.startsWith("https://") ||
                            accessibleNameLower.startsWith("www."))) {

                violations.add(new Violation.Builder()
                                       .setRuleId(getId())
                                       .setMessage("Link text contains a URL instead of descriptive text")
                                       .setElement(link.outerHtml())
                                       .setXpath(getXPath(link))
                                       .setSeverity("moderate")
                                       .setAutoFixable(false)
                                       .setRemediation("Replace the URL with descriptive text that explains the link purpose")
                                       .build());
            }

            // Check for duplicate links with different text
            String href = link.attr("href");
            Elements duplicateLinks = document.select("a[href=" + escapeSelector(href) + "]");
            if (duplicateLinks.size() > 1) {
                Set<String> linkTexts = new HashSet<>();
                for (Element dup : duplicateLinks) {
                    linkTexts.add(dup.text().trim());
                }

                if (linkTexts.size() > 1) {
                    violations.add(new Violation.Builder()
                                           .setRuleId(getId())
                                           .setMessage("Multiple links go to the same URL but have different link text")
                                           .setElement(link.outerHtml())
                                           .setXpath(getXPath(link))
                                           .setSeverity("minor")
                                           .setAutoFixable(false)
                                           .setRemediation("Use consistent link text for links that go to the same URL")
                                           .build());
                }
            }

            // Check for adjacent links with the same text but different URLs
            Elements siblings = link.parent().children();
            for (Element sibling : siblings) {
                if (sibling.tagName().equals("a") && sibling != link) {
                    String siblingText = sibling.text().trim();
                    String siblingHref = sibling.attr("href");

                    if (linkText.equals(siblingText) && !href.equals(siblingHref)) {
                        violations.add(new Violation.Builder()
                                               .setRuleId(getId())
                                               .setMessage("Adjacent links have the same text but different destinations")
                                               .setElement(link.parent().outerHtml())
                                               .setXpath(getXPath(link.parent()))
                                               .setSeverity("moderate")
                                               .setAutoFixable(false)
                                               .setRemediation("Differentiate link text to clarify the distinct destinations")
                                               .build());
                        break; // Avoid duplicate violations
                    }
                }
            }

            // Check for links opening in new window without warning
            if (link.hasAttr("target") && link.attr("target").equals("_blank") &&
                    !link.hasAttr("aria-label") && !link.hasAttr("title") &&
                    !accessibleNameLower.contains("new window") && !accessibleNameLower.contains("new tab")) {

                violations.add(new Violation.Builder()
                                       .setRuleId(getId())
                                       .setMessage("Link opens in new window without warning")
                                       .setElement(link.outerHtml())
                                       .setXpath(getXPath(link))
                                       .setSeverity("moderate")
                                       .setAutoFixable(true)
                                       .setRemediation("Add indication that link opens in a new window via aria-label or title")
                                       .build());
            }
        }

        return violations;
    }

    @Override
    public boolean canAutoFix() {
        return true; // For new window warnings only
    }

    @Override
    public int applyFixes(Document document, List<Violation> violations) {
        int fixes = 0;

        for (Violation violation : violations) {
            if (violation.getRuleId().equals(getId()) && violation.isAutoFixable()) {
                // Only the "new window" issue is auto-fixable
                if (violation.getMessage().contains("opens in new window without warning")) {
                    String xpath = violation.getXpath();
                    Elements elements = findElementsByXPath(document, xpath);

                    for (Element link : elements) {
                        if (link.hasAttr("target") && link.attr("target").equals("_blank")) {
                            // Add indication to title or aria-label
                            if (link.hasAttr("title")) {
                                String currentTitle = link.attr("title");
                                if (!currentTitle.contains("new window") && !currentTitle.contains("new tab")) {
                                    link.attr("title", currentTitle + " (opens in a new window)");
                                }
                            } else {
                                String linkText = link.text().trim();
                                link.attr("title", linkText + " (opens in a new window)");
                            }

                            // Also add a visual indicator using an icon if possible
                            link.append(" <span class=\"sr-only\">(opens in a new window)</span>");

                            fixes++;
                        }
                    }
                }
            }
        }

        return fixes;
    }

    private boolean isHidden(Element element) {
        // Check element and all parents for hidden attributes or styles
        if (element.hasAttr("hidden") ||
                element.hasAttr("aria-hidden") && element.attr("aria-hidden").equals("true") ||
                element.hasAttr("style") && element.attr("style").contains("display: none") ||
                element.hasAttr("style") && element.attr("style").contains("visibility: hidden")) {
            return true;
        }

        for (Element parent : element.parents()) {
            if (parent.hasAttr("hidden") ||
                    parent.hasAttr("aria-hidden") && parent.attr("aria-hidden").equals("true") ||
                    parent.hasAttr("style") && parent.attr("style").contains("display: none") ||
                    parent.hasAttr("style") && parent.attr("style").contains("visibility: hidden")) {
                return true;
            }
        }

        return false;
    }

    private boolean isGenericLinkText(String linkText) {
        Set<String> genericTerms = new HashSet<>(Arrays.asList(
                "click here", "click", "here", "more", "read more", "details", "learn more",
                "this page", "this link", "this", "link", "go", "go to", "navigate", "open",
                "show", "view", "see", "check", "check this out", "check it out", "visit",
                "visit this", "right here", "see here", "see this", "view this", "page",
                "website", "web page", "site", "information", "info"
        ));

        if (genericTerms.contains(linkText)) {
            return true;
        }

        // Check for patterns like "click here to..."
        for (String term : Arrays.asList("click here", "click", "here", "link")) {
            if (linkText.startsWith(term + " to ") || linkText.startsWith(term + " for ")) {
                return true;
            }
        }

        return false;
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

    private String escapeSelector(String value) {
        // Escape special characters in CSS selectors
        return value.replace("'", "\\'").replace("\"", "\\\"");
    }

    private Elements findElementsByXPath(Document document, String xpath) {
        // Simplified implementation
        if (xpath.endsWith("/a") || xpath.contains("/a[")) {
            // Target specific problems
            if (xpath.contains("opens in new window")) {
                return document.select("a[target=_blank]");
            } else if (xpath.contains("generic")) {
                return document.select("a:contains(click here), a:contains(more), a:contains(details)");
            } else if (xpath.contains("URL")) {
                return document.select("a:matches(^https?://)");
            } else {
                return document.select("a");
            }
        } else {
            return new Elements();
        }
    }
}
