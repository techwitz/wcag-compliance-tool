package com.techwitz.accessibility.rule;

import com.techwitz.accessibility.ComplianceConfig;
import com.techwitz.accessibility.Violation;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Engine for evaluating WCAG rules against HTML documents
 */
public class WcagRuleEngine {
    private static final Logger LOGGER = Logger.getLogger(WcagRuleEngine.class.getName());

    private final List<WcagRule> rules = new ArrayList<>();

    public WcagRuleEngine(ComplianceConfig config) {
        // Add standard built-in rules
        initializeStandardRules(config);

        // Add custom rules from config
        rules.addAll(config.getCustomRules());

        LOGGER.info("Rule engine initialized with " + rules.size() + " rules");
    }

    private void initializeStandardRules(ComplianceConfig config) {
        // Level A rules (essential for basic accessibility)
        rules.add(new ImageAltTextRule());
        rules.add(new FormLabelRule());
        rules.add(new HeadingStructureRule());
        rules.add(new LinkPurposeRule());
        rules.add(new TableHeadersRule());
        rules.add(new ColorContrastRule());

        // Level AA rules (enhanced accessibility)
        if (config.includeLevel2Rules()) {
            rules.add(new KeyboardAccessibilityRule());
            rules.add(new LanguageAttributeRule());
            rules.add(new ErrorIdentificationRule());
            rules.add(new FocusOrderRule());
        }

        // Level AAA rules (comprehensive accessibility)
        if (config.includeLevel3Rules()) {
            rules.add(new SignLanguageRule());
            rules.add(new ReadingLevelRule());
            rules.add(new PronunciationRule());
        }
    }

    /**
     * Evaluates all rules against a document
     * @param document Document to evaluate
     * @return List of all violations found
     */
    public List<Violation> evaluateRules(Document document) {
        List<Violation> allViolations = new ArrayList<>();

        for (WcagRule rule : rules) {
            try {
                List<Violation> violations = rule.evaluate(document);
                if (!violations.isEmpty()) {
                    LOGGER.info("Rule " + rule.getId() + " found " + violations.size() + " violations");
                    allViolations.addAll(violations);
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error evaluating rule " + rule.getId(), e);
            }
        }

        return allViolations;
    }

    /**
     * Gets all rules managed by this engine
     * @return List of rules
     */
    public List<WcagRule> getRules() {
        return Collections.unmodifiableList(rules);
    }
}

