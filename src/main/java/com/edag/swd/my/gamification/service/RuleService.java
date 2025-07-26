package com.edag.swd.my.gamification.service;

import com.edag.swd.my.gamification.config.RuleConfig;
import com.edag.swd.my.gamification.engine.EntityRuleEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Service for managing and executing rules in the gamification system.
 * This service delegates most of its work to the EntityRuleEngine.
 */
@Service
public class RuleService {
    private final EntityRuleEngine entityRuleEngine;

    @Autowired
    public RuleService(EntityRuleEngine entityRuleEngine) {
        this.entityRuleEngine = entityRuleEngine;
    }

    /**
     * Loads rules from a JSON file.
     *
     * @param resourcePath Path to the JSON file containing rules
     * @throws Exception If there's an error loading the rules
     */
    public void loadRules(String resourcePath) throws Exception {
        entityRuleEngine.loadRules(resourcePath);
    }

    /**
     * Processes an event with the given action type and participants.
     *
     * @param actionType   The type of action to process
     * @param participants Map of participant roles to person IDs
     */
    @Transactional
    public void processEvent(String actionType, Map<String, String> participants) {
        entityRuleEngine.processEvent(actionType, participants);
    }

    /**
     * Gets the loaded rules.
     *
     * @return Map of rule names to rule configurations
     */
    public Map<String, RuleConfig> getRules() {
        return entityRuleEngine.getRules();
    }
}