package com.techwitz.accessibility.rule;

import com.techwitz.accessibility.Violation;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Rule for color contrast
 */
public class ColorContrastRule implements WcagRule {
    // Contrast ratio thresholds from WCAG
    private static final double CONTRAST_THRESHOLD_NORMAL = 4.5; // For normal text
    private static final double CONTRAST_THRESHOLD_LARGE = 3.0;  // For large text (18pt or 14pt bold)

    @Override
    public String getId() {
        return "1.4.3-color-contrast";
    }

    @Override
    public String getDescription() {
        return "Text must have sufficient contrast with its background";
    }

    @Override
    public String getSuccessCriterion() {
        return "1.4.3 Contrast (Minimum)";
    }

    @Override
    public String getLevel() {
        return "AA";
    }

    @Override
    public List<Violation> evaluate(Document document) {
        List<Violation> violations = new ArrayList<>();

        // Note: Full contrast checking requires CSS processing and rendering
        // This implementation checks only inline styles for contrast issues

        Elements textElements = document.select("p, h1, h2, h3, h4, h5, h6, span, div, a, button, label, li");
        for (Element element : textElements) {
            // Skip elements with no text
            if (element.text().trim().isEmpty()) {
                continue;
            }

            // Skip hidden elements
            if (isHidden(element)) {
                continue;
            }

            if (element.hasAttr("style")) {
                String style = element.attr("style");

                // Extract color and background-color from inline style
                String foregroundColor = extractCssProperty(style, "color");
                String backgroundColor = extractCssProperty(style, "background-color");

                // Check if we have both colors for comparison
                if (!foregroundColor.isEmpty() && !backgroundColor.isEmpty()) {
                    boolean isLargeText = isLargeText(element);
                    double contrastRatio = calculateContrastRatio(foregroundColor, backgroundColor);
                    double threshold = isLargeText ? CONTRAST_THRESHOLD_LARGE : CONTRAST_THRESHOLD_NORMAL;

                    if (contrastRatio < threshold) {
                        violations.add(new Violation.Builder()
                                               .setRuleId(getId())
                                               .setMessage(String.format(
                                                       "Insufficient color contrast ratio: %.2f (minimum should be %.1f)",
                                                       contrastRatio, threshold
                                               ))
                                               .setElement(element.outerHtml())
                                               .setXpath(getXPath(element))
                                               .setSeverity("serious")
                                               .setAutoFixable(true)
                                               .setRemediation(String.format(
                                                       "Increase contrast between text color %s and background color %s to at least %.1f:1",
                                                       foregroundColor, backgroundColor, threshold
                                               ))
                                               .build());
                    }
                }
                // Check for known problematic colors
                else if (!foregroundColor.isEmpty() && isLowContrastColor(foregroundColor)) {
                    violations.add(new Violation.Builder()
                                           .setRuleId(getId())
                                           .setMessage("Potentially low contrast text color: " + foregroundColor)
                                           .setElement(element.outerHtml())
                                           .setXpath(getXPath(element))
                                           .setSeverity("moderate")
                                           .setAutoFixable(true)
                                           .setRemediation("Ensure text has sufficient contrast with its background")
                                           .build());
                }
            }

            // Check for elements with transparent backgrounds that may have contrast issues
            if ((!element.hasAttr("style") ||
                    (element.hasAttr("style") && !element.attr("style").contains("background"))) &&
                    !hasBackgroundColorParent(element)) {

                // Check if the element has a light text color that might not contrast with white
                String foregroundColor = "";
                if (element.hasAttr("style")) {
                    foregroundColor = extractCssProperty(element.attr("style"), "color");
                }

                if (!foregroundColor.isEmpty() && isLowContrastColor(foregroundColor)) {
                    violations.add(new Violation.Builder()
                                           .setRuleId(getId())
                                           .setMessage("Light text color without specified background may have contrast issues")
                                           .setElement(element.outerHtml())
                                           .setXpath(getXPath(element))
                                           .setSeverity("moderate")
                                           .setAutoFixable(true)
                                           .setRemediation("Either darken the text color or specify a dark background color")
                                           .build());
                }
            }

            // Check for color-only indications
            if (element.hasClass("text-danger") || element.hasClass("text-warning") ||
                    element.hasClass("text-success") || element.hasClass("text-info") ||
                    element.hasClass("red") || element.hasClass("green") || element.hasClass("blue") ||
                    (element.hasAttr("style") &&
                            (element.attr("style").contains("red") ||
                                    element.attr("style").contains("green") ||
                                    element.attr("style").contains("blue")))) {

                // Check if there's a non-color indicator
                boolean hasNonColorIndicator = element.hasAttr("aria-label") ||
                        element.hasAttr("title") ||
                        element.html().contains("*") ||
                        element.html().contains("!") ||
                        element.select("i.fa, i.icon, span.icon").size() > 0;

                if (!hasNonColorIndicator) {
                    violations.add(new Violation.Builder()
                                           .setRuleId(getId())
                                           .setMessage("Information conveyed through color alone")
                                           .setElement(element.outerHtml())
                                           .setXpath(getXPath(element))
                                           .setSeverity("moderate")
                                           .setAutoFixable(false)
                                           .setRemediation("Add non-color indicators (icons, patterns, text) to supplement color")
                                           .build());
                }
            }
        }

        // Check for color contrast in form controls
        Elements formControls = document.select("input, select, textarea, button");
        for (Element control : formControls) {
            if (control.hasAttr("style")) {
                String style = control.attr("style");
                String foregroundColor = extractCssProperty(style, "color");
                String backgroundColor = extractCssProperty(style, "background-color");

                // Only check if we have both colors
                if (!foregroundColor.isEmpty() && !backgroundColor.isEmpty()) {
                    double contrastRatio = calculateContrastRatio(foregroundColor, backgroundColor);
                    if (contrastRatio < CONTRAST_THRESHOLD_NORMAL) {
                        violations.add(new Violation.Builder()
                                               .setRuleId(getId())
                                               .setMessage(String.format(
                                                       "Form control has insufficient color contrast ratio: %.2f (minimum should be 4.5)",
                                                       contrastRatio
                                               ))
                                               .setElement(control.outerHtml())
                                               .setXpath(getXPath(control))
                                               .setSeverity("serious")
                                               .setAutoFixable(true)
                                               .setRemediation("Increase contrast between text and background colors")
                                               .build());
                    }
                }
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
                    if (element.hasAttr("style")) {
                        String style = element.attr("style");
                        String foregroundColor = extractCssProperty(style, "color");
                        String backgroundColor = extractCssProperty(style, "background-color");

                        // If we have both colors, calculate better contrasting colors
                        if (!foregroundColor.isEmpty() && !backgroundColor.isEmpty()) {
                            // Determine if background is light or dark
                            boolean isBackgroundDark = isColorDark(backgroundColor);

                            // Choose appropriate high-contrast text color
                            String newForegroundColor = isBackgroundDark ? "#ffffff" : "#000000";

                            // Update the style attribute
                            style = replaceCssProperty(style, "color", newForegroundColor);
                            element.attr("style", style);
                            fixes++;
                        }
                        // If we only have foreground color
                        else if (!foregroundColor.isEmpty()) {
                            // For light text, we add a dark background
                            if (isColorLight(foregroundColor)) {
                                style += "; background-color: #000000;";
                            }
                            // For dark text, we add a light background
                            else {
                                style += "; background-color: #ffffff;";
                            }

                            element.attr("style", style);
                            fixes++;
                        }
                        // If only background color without text color
                        else if (!backgroundColor.isEmpty()) {
                            // Determine appropriate text color based on background
                            boolean isBackgroundDark = isColorDark(backgroundColor);
                            String newForegroundColor = isBackgroundDark ? "#ffffff" : "#000000";

                            style += "; color: " + newForegroundColor + ";";
                            element.attr("style", style);
                            fixes++;
                        }
                    }
                    // If no style, add appropriate contrast styles
                    else {
                        // Default to dark text on light background (most common)
                        element.attr("style", "color: #000000; background-color: #ffffff;");
                        fixes++;
                    }
                }
            }
        }

        return fixes;
    }

    private boolean isHidden(Element element) {
        // Check element and parents for hidden attributes or styles
        if (element.hasAttr("hidden") ||
                element.hasAttr("aria-hidden") && element.attr("aria-hidden").equals("true") ||
                element.hasAttr("style") && (
                        element.attr("style").contains("display: none") ||
                                element.attr("style").contains("visibility: hidden") ||
                                element.attr("style").contains("opacity: 0")
                )) {
            return true;
        }

        for (Element parent : element.parents()) {
            if (parent.hasAttr("hidden") ||
                    parent.hasAttr("aria-hidden") && parent.attr("aria-hidden").equals("true") ||
                    parent.hasAttr("style") && (
                            parent.attr("style").contains("display: none") ||
                                    parent.attr("style").contains("visibility: hidden") ||
                                    parent.attr("style").contains("opacity: 0")
                    )) {
                return true;
            }
        }

        return false;
    }

    private boolean hasBackgroundColorParent(Element element) {
        // Check if any parent has a background color set
        for (Element parent : element.parents()) {
            if (parent.hasAttr("style") &&
                    parent.attr("style").contains("background-color")) {
                return true;
            }

            if (parent.hasClass("bg-dark") || parent.hasClass("bg-primary") ||
                    parent.hasClass("bg-secondary") || parent.hasClass("bg-info") ||
                    parent.hasClass("bg-success") || parent.hasClass("bg-danger") ||
                    parent.hasClass("bg-warning") || parent.hasClass("bg-light")) {
                return true;
            }
        }

        return false;
    }

    private boolean isLargeText(Element element) {
        // Check if element has large text (18pt or 14pt bold)
        String tagName = element.tagName();
        if (tagName.equals("h1") || tagName.equals("h2")) {
            return true; // Heading 1 and 2 are typically large text
        }

        if (element.hasAttr("style")) {
            String style = element.attr("style");

            // Extract font-size and font-weight
            String fontSize = extractCssProperty(style, "font-size");
            String fontWeight = extractCssProperty(style, "font-weight");

            // Check if font size is at least 18pt/24px, or at least 14pt/18.5px and bold
            if (!fontSize.isEmpty()) {
                if (fontSize.endsWith("pt")) {
                    double size = parseSize(fontSize);
                    if (size >= 18) {
                        return true;
                    }
                    if (size >= 14 && isBold(fontWeight)) {
                        return true;
                    }
                } else if (fontSize.endsWith("px")) {
                    double size = parseSize(fontSize);
                    if (size >= 24) {
                        return true;
                    }
                    if (size >= 18.5 && isBold(fontWeight)) {
                        return true;
                    }
                } else if (fontSize.endsWith("em") || fontSize.endsWith("rem")) {
                    double size = parseSize(fontSize);
                    if (size >= 1.5) { // Roughly 24px at default browser font size
                        return true;
                    }
                    if (size >= 1.2 && isBold(fontWeight)) { // Roughly 18.5px
                        return true;
                    }
                }
            }
        }

        // Check for large text CSS classes
        if (element.hasClass("display-1") || element.hasClass("display-2") ||
                element.hasClass("display-3") || element.hasClass("display-4") ||
                element.hasClass("large") || element.hasClass("x-large") ||
                element.hasClass("xx-large")) {
            return true;
        }

        return false;
    }

    private double parseSize(String sizeStr) {
        try {
            return Double.parseDouble(sizeStr.replaceAll("[^0-9.]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private boolean isBold(String fontWeight) {
        if (fontWeight.isEmpty()) {
            return false;
        }

        try {
            int weight = Integer.parseInt(fontWeight);
            return weight >= 700;
        } catch (NumberFormatException e) {
            return fontWeight.equals("bold") || fontWeight.equals("bolder");
        }
    }

    private String extractCssProperty(String style, String property) {
        // Simple CSS property extractor
        Pattern pattern = Pattern.compile(property + "\\s*:\\s*([^;]+)");
        java.util.regex.Matcher matcher = pattern.matcher(style);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "";
    }

    private String replaceCssProperty(String style, String property, String value) {
        // Replace CSS property value
        Pattern pattern = Pattern.compile("(" + property + "\\s*:\\s*)([^;]+)");
        java.util.regex.Matcher matcher = pattern.matcher(style);
        if (matcher.find()) {
            return matcher.replaceFirst("$1" + value);
        }
        return style;
    }

    private boolean isLowContrastColor(String color) {
        // Check for known low-contrast colors
        color = color.toLowerCase().trim();

        // Light colors that may have contrast issues on white backgrounds
        String[] lightColors = {
                "#fff", "#ffffff", "white",
                "#eee", "#eeeeee",
                "#ddd", "#dddddd",
                "#ccc", "#cccccc",
                "#aaa", "#aaaaaa",
                "lightgray", "lightgrey", "gainsboro",
                "beige", "ivory", "cornsilk", "linen",
                "lightyellow", "lightcyan", "lightblue", "lavender",
                "mistyrose", "lemonchiffon", "honeydew", "mintcream"
        };

        for (String lightColor : lightColors) {
            if (color.equals(lightColor)) {
                return true;
            }
        }

        // Check RGB/RGBA format
        if (color.startsWith("rgb")) {
            // Extract RGB values (simplified, doesn't handle all cases)
            Pattern pattern = Pattern.compile("\\d+");
            java.util.regex.Matcher matcher = pattern.matcher(color);
            List<Integer> values = new ArrayList<>();

            while (matcher.find() && values.size() < 3) {
                values.add(Integer.parseInt(matcher.group()));
            }

            if (values.size() >= 3) {
                int r = values.get(0);
                int g = values.get(1);
                int b = values.get(2);

                // Check if color is light (simplified algorithm)
                if (r > 220 && g > 220 && b > 220) {
                    return true;
                }
            }
        }

        return false;
    }

    private double calculateContrastRatio(String color1, String color2) {
        // Convert colors to relative luminance
        double luminance1 = getRelativeLuminance(color1);
        double luminance2 = getRelativeLuminance(color2);

        // Calculate contrast ratio
        double lighter = Math.max(luminance1, luminance2);
        double darker = Math.min(luminance1, luminance2);

        return (lighter + 0.05) / (darker + 0.05);
    }

    private double getRelativeLuminance(String color) {
        // Extract RGB values from various color formats
        int[] rgb = parseColorToRGB(color);

        if (rgb != null) {
            // Normalize RGB values to 0-1 range
            double r = rgb[0] / 255.0;
            double g = rgb[1] / 255.0;
            double b = rgb[2] / 255.0;

            // Apply gamma correction
            r = (r <= 0.03928) ? r / 12.92 : Math.pow((r + 0.055) / 1.055, 2.4);
            g = (g <= 0.03928) ? g / 12.92 : Math.pow((g + 0.055) / 1.055, 2.4);
            b = (b <= 0.03928) ? b / 12.92 : Math.pow((b + 0.055) / 1.055, 2.4);

            // Calculate luminance
            return 0.2126 * r + 0.7152 * g + 0.0722 * b;
        }

        return 0.5; // Default if color can't be parsed
    }

    private int[] parseColorToRGB(String color) {
        color = color.toLowerCase().trim();

        // Parse HTML color names
        Map<String, int[]> colorMap = new HashMap<>();
        colorMap.put("black", new int[] {0, 0, 0});
        colorMap.put("white", new int[] {255, 255, 255});
        colorMap.put("red", new int[] {255, 0, 0});
        colorMap.put("green", new int[] {0, 128, 0});
        colorMap.put("blue", new int[] {0, 0, 255});
        colorMap.put("yellow", new int[] {255, 255, 0});
        colorMap.put("purple", new int[] {128, 0, 128});
        colorMap.put("gray", new int[] {128, 128, 128});
        colorMap.put("grey", new int[] {128, 128, 128});
        colorMap.put("lightgray", new int[] {211, 211, 211});
        colorMap.put("lightgrey", new int[] {211, 211, 211});
        colorMap.put("darkgray", new int[] {169, 169, 169});
        colorMap.put("darkgrey", new int[] {169, 169, 169});
        colorMap.put("brown", new int[] {165, 42, 42});
        colorMap.put("orange", new int[] {255, 165, 0});
        colorMap.put("pink", new int[] {255, 192, 203});
        colorMap.put("gold", new int[] {255, 215, 0});
        colorMap.put("silver", new int[] {192, 192, 192});
        colorMap.put("navy", new int[] {0, 0, 128});
        colorMap.put("teal", new int[] {0, 128, 128});

        if (colorMap.containsKey(color)) {
            return colorMap.get(color);
        }

        // Parse hex colors
        if (color.startsWith("#")) {
            try {
                if (color.length() == 4) { // #RGB
                    int r = Integer.parseInt(color.substring(1, 2) + color.substring(1, 2), 16);
                    int g = Integer.parseInt(color.substring(2, 3) + color.substring(2, 3), 16);
                    int b = Integer.parseInt(color.substring(3, 4) + color.substring(3, 4), 16);
                    return new int[] {r, g, b};
                } else if (color.length() == 7) { // #RRGGBB
                    int r = Integer.parseInt(color.substring(1, 3), 16);
                    int g = Integer.parseInt(color.substring(3, 5), 16);
                    int b = Integer.parseInt(color.substring(5, 7), 16);
                    return new int[] {r, g, b};
                }
            } catch (NumberFormatException e) {
                // Parsing failed, fall through to next format
            }
        }

        // Parse rgb/rgba colors
        if (color.startsWith("rgb")) {
            Pattern pattern = Pattern.compile("\\d+");
            java.util.regex.Matcher matcher = pattern.matcher(color);
            List<Integer> values = new ArrayList<>();

            while (matcher.find() && values.size() < 3) {
                values.add(Integer.parseInt(matcher.group()));
            }

            if (values.size() >= 3) {
                return new int[] {values.get(0), values.get(1), values.get(2)};
            }
        }

        // Parse HSL colors (simplified)
        if (color.startsWith("hsl")) {
            Pattern pattern = Pattern.compile("\\d+");
            java.util.regex.Matcher matcher = pattern.matcher(color);
            List<Integer> values = new ArrayList<>();

            while (matcher.find() && values.size() < 3) {
                values.add(Integer.parseInt(matcher.group()));
            }

            if (values.size() >= 3) {
                // Convert HSL to RGB (simplified implementation)
                return hslToRgb(values.get(0), values.get(1), values.get(2));
            }
        }

        return null; // Unable to parse color
    }

    private int[] hslToRgb(int h, int s, int l) {
        // Convert HSL to RGB
        // This is a simplified implementation
        double hue = h / 360.0;
        double saturation = s / 100.0;
        double lightness = l / 100.0;

        double r, g, b;

        if (saturation == 0) {
            // Achromatic (gray)
            r = g = b = lightness;
        } else {
            double q = lightness < 0.5 ? lightness * (1 + saturation) : lightness + saturation - lightness * saturation;
            double p = 2 * lightness - q;
            r = hueToRgb(p, q, hue + 1.0/3.0);
            g = hueToRgb(p, q, hue);
            b = hueToRgb(p, q, hue - 1.0/3.0);
        }

        return new int[] {(int)(r * 255), (int)(g * 255), (int)(b * 255)};
    }

    private double hueToRgb(double p, double q, double t) {
        if (t < 0) t += 1;
        if (t > 1) t -= 1;

        if (t < 1.0/6.0) return p + (q - p) * 6 * t;
        if (t < 1.0/2.0) return q;
        if (t < 2.0/3.0) return p + (q - p) * (2.0/3.0 - t) * 6;

        return p;
    }

    private boolean isColorDark(String color) {
        double luminance = getRelativeLuminance(color);
        return luminance < 0.5;
    }

    private boolean isColorLight(String color) {
        double luminance = getRelativeLuminance(color);
        return luminance >= 0.5;
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
        // This is a simplified implementation - in a real app, would use a proper XPath engine

        // Find all elements with style attributes containing color-related properties
        if (xpath.contains("color-contrast") || xpath.contains("Color contrast")) {
            return document.select("*[style*=color]");
        }

        // For specific elements
        if (xpath.contains("[@id=")) {
            int startIdx = xpath.indexOf("[@id='") + 6;
            int endIdx = xpath.indexOf("']", startIdx);
            if (startIdx >= 6 && endIdx > startIdx) {
                String id = xpath.substring(startIdx, endIdx);
                return document.select("#" + id);
            }
        }

        // By element tag
        String tag = xpath.substring(xpath.lastIndexOf("/") + 1);
        if (tag.matches("[a-z0-9]+")) {
            return document.select(tag + "[style*=color]");
        }

        return document.select("*[style*=color]");
    }
}
