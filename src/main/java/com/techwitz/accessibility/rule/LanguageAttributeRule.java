package com.techwitz.accessibility.rule;

import com.techwitz.accessibility.Violation;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Rule for language attribute
 */
public class LanguageAttributeRule implements WcagRule {
    @Override
    public String getId() {
        return "3.1.1-language";
    }

    @Override
    public String getDescription() {
        return "Page must have language attribute";
    }

    @Override
    public String getSuccessCriterion() {
        return "3.1.1 Language of Page";
    }

    @Override
    public String getLevel() {
        return "A";
    }

    @Override
    public List<Violation> evaluate(Document document) {
        List<Violation> violations = new ArrayList<>();

        // Check for html lang attribute
        Element html = document.select("html").first();
        if (html == null || !html.hasAttr("lang") || html.attr("lang").trim().isEmpty()) {
            violations.add(new Violation.Builder()
                                   .setRuleId(getId())
                                   .setMessage("HTML element missing lang attribute")
                                   .setElement(html != null ? html.outerHtml() : "<html>")
                                   .setXpath("/html")
                                   .setSeverity("serious")
                                   .setAutoFixable(true)
                                   .setRemediation("Add lang attribute to the html element (e.g., lang='en')")
                                   .build());
        } else {
            // Check if lang attribute value is valid
            String lang = html.attr("lang").trim();
            if (!isValidLanguageTag(lang)) {
                violations.add(new Violation.Builder()
                                       .setRuleId(getId())
                                       .setMessage("HTML element has invalid lang attribute value: " + lang)
                                       .setElement(html.outerHtml())
                                       .setXpath("/html")
                                       .setSeverity("serious")
                                       .setAutoFixable(true)
                                       .setRemediation("Replace with a valid language tag (e.g., 'en', 'en-US', 'fr', 'es')")
                                       .build());
            }
        }

        // Check for content in different languages without lang attribute
        checkForeignLanguageContent(document, violations);

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
                if (violation.getMessage().contains("HTML element missing lang")) {
                    Element html = document.select("html").first();
                    if (html != null) {
                        html.attr("lang", "en");  // Default to English
                        fixes++;
                    }
                } else if (violation.getMessage().contains("invalid lang attribute")) {
                    Element html = document.select("html").first();
                    if (html != null) {
                        html.attr("lang", "en");  // Default to English
                        fixes++;
                    }
                }
                // We don't auto-fix foreign language content as it requires content knowledge
            }
        }

        return fixes;
    }

    private void checkForeignLanguageContent(Document document, List<Violation> violations) {
        // This is a simplified check that looks for common non-English phrases
        // In a real implementation, you would use language detection algorithms

        // Get main document language
        Element html = document.select("html").first();
        String docLang = html != null && html.hasAttr("lang") ? html.attr("lang").trim() : "";

        // Define some common phrases in various languages (simplified example)
        Map<String, String> foreignPhrases = new HashMap<>();
        foreignPhrases.put("bonjour", "fr");
        foreignPhrases.put("au revoir", "fr");
        foreignPhrases.put("hola", "es");
        foreignPhrases.put("gracias", "es");
        foreignPhrases.put("guten tag", "de");
        foreignPhrases.put("auf wiedersehen", "de");
        foreignPhrases.put("ciao", "it");
        foreignPhrases.put("arrivederci", "it");

        // Check text nodes for foreign phrases
        Elements textContainers = document.select("p, span, div, h1, h2, h3, h4, h5, h6, li, td, th, blockquote");
        for (Element container : textContainers) {
            // Skip elements that already have lang attribute
            if (container.hasAttr("lang")) {
                continue;
            }

            String text = container.text().toLowerCase();

            for (Map.Entry<String, String> entry : foreignPhrases.entrySet()) {
                String phrase = entry.getKey();
                String lang = entry.getValue();

                // Skip if phrase language matches document language
                if (docLang.startsWith(lang)) {
                    continue;
                }

                // Check if the container has this foreign phrase
                if (text.contains(phrase)) {
                    violations.add(new Violation.Builder()
                                           .setRuleId(getId())
                                           .setMessage("Content may contain foreign language text without lang attribute")
                                           .setElement(container.outerHtml())
                                           .setXpath(getXPath(container))
                                           .setSeverity("moderate")
                                           .setAutoFixable(false)
                                           .setRemediation("Add lang='" + lang + "' attribute to elements containing foreign language text")
                                           .build());

                    // Break to avoid multiple violations for the same element
                    break;
                }
            }
        }
    }

    private boolean isValidLanguageTag(String lang) {
        // Simplified check for valid language tags
        // RFC 5646 language tags are more complex
        return lang.matches("^[a-zA-Z]{2,3}(-[a-zA-Z]{2,3})?$");
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
}
