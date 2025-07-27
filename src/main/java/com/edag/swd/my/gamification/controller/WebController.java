package com.edag.swd.my.gamification.controller;

import com.edag.swd.my.gamification.config.ConditionConfig;
import com.edag.swd.my.gamification.config.OutcomeConfig;
import com.edag.swd.my.gamification.config.RuleConfig;
import com.edag.swd.my.gamification.entity.Group;
import com.edag.swd.my.gamification.entity.Person;
import com.edag.swd.my.gamification.service.GroupService;
import com.edag.swd.my.gamification.service.PersonService;
import com.edag.swd.my.gamification.service.RuleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

@Controller("/api/")
public class WebController {

    private final PersonService personService;
    private final GroupService groupService;
    private final RuleService ruleService;

    @Autowired
    public WebController(PersonService personService, GroupService groupService, RuleService ruleService) {
        this.personService = personService;
        this.groupService = groupService;
        this.ruleService = ruleService;
    }

    @GetMapping
    public String home() {
        return "index";
    }

    @GetMapping("/persons")
    public String listPersons(Model model) {
        List<Person> persons = personService.getAllPersons();

        // Sort persons by total accumulated points (highest first)
        List<Person> sortedPersons = new ArrayList<>(persons);
        sortedPersons.sort(Comparator.comparingInt(Person::getTotalAccumulatedPoints).reversed());

        model.addAttribute("persons", sortedPersons);
        return "persons/list";
    }

    @GetMapping("/groups")
    public String listGroups(Model model) {
        List<Group> allGroups = groupService.getAllGroups();
        model.addAttribute("groups", allGroups);

        List<Group> sortedGroupsForRanking = new ArrayList<>(allGroups);
        sortedGroupsForRanking.sort(Comparator.comparingInt(Group::getTotalGroupPoints).reversed());
        model.addAttribute("rankedGroups", sortedGroupsForRanking);
        return "groups/list";
    }

    @GetMapping("/rules")
    public String listRules(Model model) {
        model.addAttribute("rules", ruleService.getRules().values());
        return "rules/list";
    }

    // Dynamic rule form handler
    @GetMapping("/rules/{ruleName}")
    public String ruleForm(@PathVariable String ruleName, Model model) {
        // Find the rule by name
        var rule = ruleService.getRules().values().stream()
                .filter(r -> r.getRuleName().replace(" ", "-").toLowerCase().equals(ruleName))
                .findFirst()
                .orElse(null);

        if (rule == null) {
            // If rule not found, redirect to rules list
            return "redirect:/rules";
        }

        // Add rule and persons to model
        model.addAttribute("rule", rule);
        model.addAttribute("persons", personService.getAllPersons());

        return "rules/rule-form";
    }

    // Dynamic rule execution handler
    @PostMapping("/rules/execute/{actionType}")
    public String executeRule(
            @PathVariable String actionType,
            @RequestParam(required = false) List<String> penaltyIds,
            @RequestParam(required = false) List<String> awardIds,
            @RequestParam(required = false) Map<String, String> allParams,
            RedirectAttributes redirectAttributes) {

        Map<String, String> participants = new HashMap<>();

        // Find the rule by action type
        RuleConfig rule = ruleService.getRules().values().stream()
                .filter(r -> r.getConditions().stream()
                        .anyMatch(c -> c.getValue().equals(actionType)))
                .findFirst()
                .orElse(null);

        // If rule is not found or not active, redirect to a rule list
        if (rule == null || !rule.isActive()) {
            redirectAttributes.addFlashAttribute("error", "Rule not found or inactive");
            return "redirect:/rules";
        }

        // Process penalty recipients
        if (penaltyIds != null && !penaltyIds.isEmpty()) {
            // Get all penalty targets from the rule's outcomes
            List<String> penaltyTargets = rule.getOutcomes().stream()
                    .filter(outcome -> "penalty".equalsIgnoreCase(outcome.getType()))
                    .map(outcome -> outcome.getTarget() != null ? outcome.getTarget() : "individual")
                    .distinct()
                    .toList();

            // If there are penalty targets, use the first one (most rules have only one)
            String penaltyTarget = penaltyTargets.isEmpty() ? "individual" : penaltyTargets.getFirst();

            // Add all penalty recipients with the appropriate target role
            for (String id : penaltyIds) {
                participants.put(penaltyTarget, id);
            }
        }

        // Process award recipients
        if (awardIds != null && !awardIds.isEmpty()) {
            // Get all award targets from the rule's outcomes
            List<String> awardTargets = rule.getOutcomes().stream()
                    .filter(outcome -> "award".equalsIgnoreCase(outcome.getType()))
                    .map(outcome -> outcome.getTarget() != null ? outcome.getTarget() : "individual")
                    .distinct()
                    .toList();

            // If there are award targets, use the first one (most rules have only one)
            String awardTarget = awardTargets.isEmpty() ? "individual" : awardTargets.getFirst();

            // Add all award recipients with the appropriate target role
            for (String id : awardIds) {
                participants.put(awardTarget, id);
            }
        }

        // Process the event
        ruleService.processEvent(actionType, participants);

        // Find the rule name from the action type
        String ruleName = ruleService.getRules().values().stream()
                .filter(r -> r.getConditions().stream()
                        .anyMatch(c -> c.getValue().equals(actionType)))
                .findFirst()
                .map(RuleConfig::getRuleName)
                .orElse("Rule");

        // Add success message
        redirectAttributes.addFlashAttribute("success", ruleName + " rule executed successfully");

        // Convert rule name to URL format
        String ruleUrl = ruleName.replace(" ", "-").toLowerCase();
        return "redirect:/rules/" + ruleUrl;
    }

    /**
     * Displays the rule creation form.
     *
     * @param model The model to add attributes to
     * @return The name of the view to render
     */
    @GetMapping("/rules/create")
    public String createRuleForm(Model model) {
        return "rules/create-rule";
    }

    /**
     * Processes the rule creation form submission.
     *
     * @param ruleJson           The JSON representation of the rule
     * @param redirectAttributes Attributes to add to the redirect
     * @return The name of the view to render
     */
    @PostMapping("/rules/save")
    public String saveRule(@RequestParam String ruleJson, RedirectAttributes redirectAttributes) {
        try {
            // Parse the rule JSON
            ObjectMapper mapper = new ObjectMapper();
            RuleConfig rule = mapper.readValue(ruleJson, RuleConfig.class);

            // Validate the rule
            if (!validateRule(rule)) {
                redirectAttributes.addFlashAttribute("error", "Invalid rule configuration");
                return "redirect:/rules/create";
            }

            // Add the rule
            boolean added = ruleService.addRule(rule);

            if (added) {
                redirectAttributes.addFlashAttribute("success", "Rule '" + rule.getRuleName() + "' created successfully");
                return "redirect:/rules";
            } else {
                redirectAttributes.addFlashAttribute("error", "A rule with the name '" + rule.getRuleName() + "' already exists");
                return "redirect:/rules/create";
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error creating rule: " + e.getMessage());
            return "redirect:/rules/create";
        }
    }

    /**
     * Validates a rule configuration.
     *
     * @param rule The rule to validate
     * @return true if the rule is valid, false otherwise
     */
    private boolean validateRule(RuleConfig rule) {
        // Check if rule name is provided
        if (rule.getRuleName() == null || rule.getRuleName().trim().isEmpty()) {
            return false;
        }

        // Check if description is provided
        if (rule.getDescription() == null || rule.getDescription().trim().isEmpty()) {
            return false;
        }

        // Check if at least one condition is provided
        if (rule.getConditions() == null || rule.getConditions().isEmpty()) {
            return false;
        }

        // Check if all conditions have a type and value
        for (ConditionConfig condition : rule.getConditions()) {
            if (condition.getType() == null || condition.getType().trim().isEmpty() ||
                    condition.getValue() == null || condition.getValue().trim().isEmpty()) {
                return false;
            }
        }

        // Check if at least one outcome is provided
        if (rule.getOutcomes() == null || rule.getOutcomes().isEmpty()) {
            return false;
        }

        // Check if all outcomes have a type, points, target, and reason
        for (OutcomeConfig outcome : rule.getOutcomes()) {
            if (outcome.getType() == null || outcome.getType().trim().isEmpty() ||
                    outcome.getTarget() == null || outcome.getTarget().trim().isEmpty() ||
                    outcome.getReason() == null || outcome.getReason().trim().isEmpty()) {
                return false;
            }
        }

        // Check if cap is valid
        if (rule.getCap() != null && rule.getCap().getMaxPoints() <= 0) {
            return false;
        }

        return true;
    }
}