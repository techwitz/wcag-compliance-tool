package com.techwitz.accessibility.rule;

import com.techwitz.accessibility.Violation;
import org.jsoup.nodes.Document;

import java.util.List;

/**
 * Represents a WCAG 2.3 rule to check for compliance
 */
public interface WcagRule {
    /**
     * Gets the unique identifier for this rule
     */
    String getId();

    /**
     * Gets the human-readable description of this rule
     */
    String getDescription();

    /**
     * Gets the WCAG success criterion this rule corresponds to
     */
    String getSuccessCriterion();

    /**
     * Gets the compliance level (A, AA, AAA)
     */
    String getLevel();

    /**
     * Evaluates the rule against a document
     * @param document Document to evaluate
     * @return List of violations found
     */
    List<Violation> evaluate(Document document);

    /**
     * Determines if this rule can automatically fix violations
     */
    boolean canAutoFix();

    /**
     * Applies fixes for violations of this rule
     * @param document Document to fix
     * @param violations List of violations to fix
     * @return Number of fixes applied
     */
    int applyFixes(Document document, List<Violation> violations);
}
