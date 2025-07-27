package com.edag.swd.my.gamification.service;

import com.edag.swd.my.gamification.config.RuleConfig;
import com.edag.swd.my.gamification.engine.RuleEngine;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing and executing rules in the gamification system.
 * This service delegates most of its work to the EntityRuleEngine.
 */
@Service
public class RuleService {
    private final RuleEngine ruleEngine;
    private final ResourceLoader resourceLoader;
    private final String rulesJsonPath;
    private final Map<String, RuleConfig> rules = new ConcurrentHashMap<>();

    @Autowired
    public RuleService(RuleEngine ruleEngine, ResourceLoader resourceLoader,
                       @Value("${rules.json.path:/rules.json}") String rulesJsonPath) {
        this.ruleEngine = ruleEngine;
        this.resourceLoader = resourceLoader;
        this.rulesJsonPath = rulesJsonPath;
    }

    /**
     * Loads rules from a JSON file.
     *
     * @param resourcePath Path to the JSON file containing rules
     * @throws Exception If there's an error loading the rules
     */
    public void loadRules(String resourcePath) throws Exception {
        ruleEngine.loadRules(resourcePath);

        // Also load rules into the local map
        ObjectMapper mapper = new ObjectMapper();
        Resource resource = resourceLoader.getResource("classpath:" + resourcePath);

        try (InputStream inputStream = resource.getInputStream()) {
            List<RuleConfig> ruleList = mapper.readValue(inputStream, new TypeReference<List<RuleConfig>>() {
            });
            ruleList.forEach(rule -> this.rules.put(rule.getRuleName(), rule));
        }
    }

    /**
     * Processes an event with the given action type and participants.
     *
     * @param actionType   The type of action to process
     * @param participants Map of participant roles to person IDs
     */
    @Transactional
    public void processEvent(String actionType, Map<String, String> participants) {
        ruleEngine.processEvent(actionType, participants);
    }

    /**
     * Gets the loaded rules.
     *
     * @return Map of rule names to rule configurations
     */
    public Map<String, RuleConfig> getRules() {
        return ruleEngine.getRules();
    }

    /**
     * Adds a new rule to the system.
     *
     * @param rule The rule to add
     * @return true if the rule was added successfully, false if a rule with the same name already exists
     * @throws Exception If there's an error saving the rules
     */
    public boolean addRule(RuleConfig rule) throws Exception {
        // Check if a rule with the same name already exists
        if (getRules().containsKey(rule.getRuleName())) {
            return false;
        }

        // Add the rule to the local map
        this.rules.put(rule.getRuleName(), rule);

        // Save the rules to the file
        saveRules();

        // Reload the rules in the rule engine
        ruleEngine.loadRules(rulesJsonPath);

        return true;
    }

    /**
     * Saves the current rules to the rules.json file.
     *
     * @throws Exception If there's an error saving the rules
     */
    private void saveRules() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        // Get all rules from the rule engine
        Map<String, RuleConfig> engineRules = getRules();

        // Merge with local rules
        Map<String, RuleConfig> allRules = new ConcurrentHashMap<>(engineRules);
        allRules.putAll(this.rules);

        // Convert the rules map to a list
        List<RuleConfig> ruleList = new ArrayList<>(allRules.values());

        // Get the file path
        Path filePath = Paths.get("src/main/resources" + rulesJsonPath);

        // Write the rules to the file
        try (OutputStream os = Files.newOutputStream(filePath)) {
            mapper.writeValue(os, ruleList);
            System.out.println("Saved " + ruleList.size() + " rules to " + filePath);
        } catch (IOException e) {
            System.err.println("Error saving rules: " + e.getMessage());
            throw e;
        }
    }
}