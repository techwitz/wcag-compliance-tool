package com.techwitz.accessibility.rule;

import com.techwitz.accessibility.Violation;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

/**
 * Rule for table headers
 */
public class TableHeadersRule implements WcagRule {
    @Override
    public String getId() {
        return "1.3.1-table-headers";
    }

    @Override
    public String getDescription() {
        return "Data tables must have proper headers";
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

        Elements tables = document.select("table");
        for (Element table : tables) {
            // Skip layout tables (those marked as presentation)
            if (isLayoutTable(table)) {
                continue;
            }

            // Check if the table has any headers
            Elements thElements = table.select("th");
            Elements headerRows = table.select("thead tr");

            if (thElements.isEmpty() && headerRows.isEmpty()) {
                // Table with no headers
                violations.add(new Violation.Builder()
                                       .setRuleId(getId())
                                       .setMessage("Table has no header cells (th) or header rows (thead)")
                                       .setElement(table.outerHtml())
                                       .setXpath(getXPath(table))
                                       .setSeverity("serious")
                                       .setAutoFixable(true)
                                       .setRemediation("Add th elements for each column or row that serves as a header")
                                       .build());
            }

            // Check for scope attributes on th elements
            for (Element th : thElements) {
                if (!th.hasAttr("scope")) {
                    violations.add(new Violation.Builder()
                                           .setRuleId(getId())
                                           .setMessage("Table header cell missing scope attribute")
                                           .setElement(th.outerHtml())
                                           .setXpath(getXPath(th))
                                           .setSeverity("moderate")
                                           .setAutoFixable(true)
                                           .setRemediation("Add scope='col' or scope='row' to the th element")
                                           .build());
                }
            }

            // Check for complex tables that need more sophisticated headers
            if (!thElements.isEmpty() && isComplexTable(table)) {
                // Check for headers and id attributes
                boolean hasHeadersAttr = false;
                Elements dataCells = table.select("td");

                for (Element td : dataCells) {
                    if (td.hasAttr("headers")) {
                        hasHeadersAttr = true;
                        break;
                    }
                }

                if (!hasHeadersAttr) {
                    violations.add(new Violation.Builder()
                                           .setRuleId(getId())
                                           .setMessage("Complex table without headers/id associations")
                                           .setElement(table.outerHtml())
                                           .setXpath(getXPath(table))
                                           .setSeverity("serious")
                                           .setAutoFixable(false)
                                           .setRemediation("Use headers and id attributes to associate data cells with headers")
                                           .build());
                }
            }

            // Check for empty header cells
            for (Element th : thElements) {
                if (th.text().trim().isEmpty() && !th.select("img[alt]").hasText()) {
                    violations.add(new Violation.Builder()
                                           .setRuleId(getId())
                                           .setMessage("Empty table header cell")
                                           .setElement(th.outerHtml())
                                           .setXpath(getXPath(th))
                                           .setSeverity("moderate")
                                           .setAutoFixable(false)
                                           .setRemediation("Add descriptive text to the header cell")
                                           .build());
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
                    if (element.tagName().equals("table")) {
                        // Fix tables with no headers
                        if (violation.getMessage().contains("no header cells")) {
                            Elements firstRow = element.select("tr").first().select("td");

                            if (!firstRow.isEmpty()) {
                                // Convert first row cells to th elements with scope="col"
                                for (Element cell : firstRow) {
                                    cell.tagName("th");
                                    cell.attr("scope", "col");

                                    // If cell is empty, add placeholder text
                                    if (cell.text().trim().isEmpty()) {
                                        cell.text("Column " + (firstRow.indexOf(cell) + 1));
                                    }

                                    fixes++;
                                }
                            }
                        }
                    } else if (element.tagName().equals("th")) {
                        // Fix th elements without scope
                        if (violation.getMessage().contains("missing scope attribute")) {
                            // Determine if it's a row or column header
                            Element parent = element.parent();
                            if (parent != null && parent.tagName().equals("tr")) {
                                Elements siblings = parent.children();
                                int index = siblings.indexOf(element);

                                // First cell in a row is often a row header
                                if (index == 0) {
                                    element.attr("scope", "row");
                                } else {
                                    element.attr("scope", "col");
                                }

                                fixes++;
                            }
                        }
                    }
                }
            }
        }

        return fixes;
    }

    private boolean isLayoutTable(Element table) {
        // Check for explicit role
        if (table.hasAttr("role") &&
                (table.attr("role").equals("presentation") || table.attr("role").equals("none"))) {
            return true;
        }

        // Check for common layout table patterns
        if (table.hasClass("layout") || table.hasClass("layout-table") ||
                table.hasClass("presentational") || table.hasClass("non-data")) {
            return true;
        }

        // Check if table is nested in a container that suggests layout purpose
        Element parent = table.parent();
        if (parent != null && (parent.hasClass("layout") || parent.hasClass("non-data"))) {
            return true;
        }

        // Check if table has very few cells (less than 3), which often indicates layout usage
        Elements cells = table.select("th, td");
        if (cells.size() <= 2 && table.select("th").isEmpty()) {
            return true;
        }

        return false;
    }

    private boolean isComplexTable(Element table) {
        // Check for rowspan or colspan attributes
        Elements cells = table.select("td, th");
        for (Element cell : cells) {
            if ((cell.hasAttr("rowspan") && !cell.attr("rowspan").equals("1")) ||
                    (cell.hasAttr("colspan") && !cell.attr("colspan").equals("1"))) {
                return true;
            }
        }

        // Check for multiple header rows/columns
        Elements headerRows = table.select("thead tr");
        if (headerRows.size() > 1) {
            return true;
        }

        Elements firstRowCells = table.select("tr:first-child th");
        Elements firstColCells = table.select("tr > th:first-child");

        // Table has both row and column headers
        if (!firstRowCells.isEmpty() && !firstColCells.isEmpty()) {
            return true;
        }

        // Check for caption, as this often indicates a more complex table
        if (!table.select("caption").isEmpty()) {
            return true;
        }

        // Check for many rows and columns (suggesting complex data relationships)
        Elements rows = table.select("tr");
        if (rows.size() >= 10) {
            // Find max number of columns
            int maxCols = 0;
            for (Element row : rows) {
                int cols = row.select("td, th").size();
                maxCols = Math.max(maxCols, cols);
            }

            if (maxCols >= 6) {
                return true; // Large tables with many rows and columns
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

    private Elements findElementsByXPath(Document document, String xpath) {
        // Simplified implementation
        if (xpath.endsWith("/table")) {
            if (xpath.contains("[@id=")) {
                // Extract ID and find by ID
                int startIdx = xpath.indexOf("[@id='") + 6;
                int endIdx = xpath.indexOf("']", startIdx);
                if (startIdx >= 6 && endIdx > startIdx) {
                    String id = xpath.substring(startIdx, endIdx);
                    return document.select("table#" + id);
                }
            } else {
                // Find tables with no headers
                return document.select("table:not(:has(th)):not(:has(thead))");
            }
        } else if (xpath.contains("/th")) {
            return document.select("th:not([scope])");
        }

        return new Elements();
    }
}
