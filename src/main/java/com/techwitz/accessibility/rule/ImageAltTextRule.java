package com.techwitz.accessibility.rule;

import com.techwitz.accessibility.Violation;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;

/**
 * Rule for ensuring images have alternative text
 */
public class ImageAltTextRule implements WcagRule {
    @Override
    public String getId() {
        return "1.1.1-img-alt";
    }

    @Override
    public String getDescription() {
        return "Images must have alt text";
    }

    @Override
    public String getSuccessCriterion() {
        return "1.1.1 Non-text Content";
    }

    @Override
    public String getLevel() {
        return "A";
    }

    @Override
    public List<Violation> evaluate(Document document) {
        List<Violation> violations = new ArrayList<>();

        Elements images = document.select("img");
        for (Element img : images) {
            if (!img.hasAttr("alt")) {
                violations.add(new Violation.Builder()
                                       .setRuleId(getId())
                                       .setMessage("Image missing alt attribute")
                                       .setElement(img.outerHtml())
                                       .setXpath(getXPath(img))
                                       .setSeverity("critical")
                                       .setAutoFixable(true)
                                       .setRemediation("Add alt attribute with descriptive text")
                                       .build());
            } else if (img.attr("alt").trim().isEmpty() && !isDecorativeImage(img)) {
                violations.add(new Violation.Builder()
                                       .setRuleId(getId())
                                       .setMessage("Image has empty alt attribute but appears to be non-decorative")
                                       .setElement(img.outerHtml())
                                       .setXpath(getXPath(img))
                                       .setSeverity("serious")
                                       .setAutoFixable(false)
                                       .setRemediation("Add descriptive alt text appropriate to the image content")
                                       .build());
            } else if (hasGenericAltText(img.attr("alt"))) {
                violations.add(new Violation.Builder()
                                       .setRuleId(getId())
                                       .setMessage("Image has generic alt text: \"" + img.attr("alt") + "\"")
                                       .setElement(img.outerHtml())
                                       .setXpath(getXPath(img))
                                       .setSeverity("moderate")
                                       .setAutoFixable(false)
                                       .setRemediation("Replace generic alt text with descriptive content")
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

                for (Element img : elements) {
                    if (!img.hasAttr("alt")) {
                        // Extract filename from src as a placeholder
                        String src = img.attr("src");
                        String placeholder = "[IMAGE DESCRIPTION NEEDED]";

                        if (!src.isEmpty()) {
                            String filename = src.substring(src.lastIndexOf('/') + 1);
                            placeholder = "Image: " + filename;
                        }

                        img.attr("alt", placeholder);
                        fixes++;
                    }
                }
            }
        }

        return fixes;
    }

    private boolean isDecorativeImage(Element img) {
        // Check common patterns that suggest decorative images
        if (img.hasAttr("role") && img.attr("role").equals("presentation")) {
            return true;
        }

        if (img.hasClass("decorative") || img.hasClass("decoration") ||
                img.hasClass("bg") || img.hasClass("background")) {
            return true;
        }

        // Check if it's a spacer or very small image
        if (img.hasAttr("width") && img.hasAttr("height")) {
            try {
                int width = Integer.parseInt(img.attr("width"));
                int height = Integer.parseInt(img.attr("height"));

                if ((width <= 3 && height <= 3) || width == 1 || height == 1) {
                    return true;
                }
            } catch (NumberFormatException e) {
                // Ignore parse errors
            }
        }

        // Check if the src contains patterns suggesting decorative images
        String src = img.attr("src").toLowerCase();
        if (src.contains("spacer") || src.contains("transparent") ||
                src.contains("pixel") || src.contains("blank")) {
            return true;
        }

        return false;
    }

    private boolean hasGenericAltText(String altText) {
        String alt = altText.toLowerCase().trim();

        // Check for common generic alt text patterns
        if (alt.isEmpty()) {
            return false; // Empty alt is not "generic" - it's handled separately
        }

        String[] genericPatterns = {
                "image", "picture", "photo", "graphic", "icon", "img", "pic",
                "placeholder", "banner", "logo", "button", "click here",
                "*", "-", "_", "image of", "picture of", "graphic of"
        };

        for (String pattern : genericPatterns) {
            if (alt.equals(pattern) || alt.startsWith(pattern + " ") || alt.endsWith(" " + pattern)) {
                return true;
            }
        }

        // Check for image filenames or URLs in alt text
        if (alt.endsWith(".jpg") || alt.endsWith(".png") || alt.endsWith(".gif") ||
                alt.endsWith(".jpeg") || alt.endsWith(".webp") || alt.endsWith(".svg") ||
                alt.contains(".com/") || alt.contains("/images/")) {
            return true;
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

    private Elements findElementsByXPath(Document document, String xpath) {
        // This is a simplified implementation - in a real app, you would use a proper XPath library
        if (xpath.contains("/img")) {
            if (xpath.contains("[@id=")) {
                int start = xpath.indexOf("[@id='") + 6;
                int end = xpath.indexOf("']", start);
                if (start >= 6 && end > start) {
                    String id = xpath.substring(start, end);
                    return document.select("img#" + id);
                }
            }
            return document.select("img:not([alt])");
        }

        return new Elements();
    }
}

