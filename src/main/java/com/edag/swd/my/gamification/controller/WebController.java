package com.edag.swd.my.gamification.controller;

import com.edag.swd.my.gamification.config.RuleConfig;
import com.edag.swd.my.gamification.entity.Group;
import com.edag.swd.my.gamification.entity.Person;
import com.edag.swd.my.gamification.service.GroupService;
import com.edag.swd.my.gamification.service.PersonService;
import com.edag.swd.my.gamification.service.RuleService;
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
            @RequestParam(required = false) String participantId,
            @RequestParam(required = false) List<String> participantIds,
            @RequestParam String targetRole,
            RedirectAttributes redirectAttributes) {

        Map<String, String> participants = new HashMap<>();

        // Handle multiple participants (for SAP Hours Offender)
        if (participantIds != null && !participantIds.isEmpty()) {
            for (String id : participantIds) {
                participants.put(targetRole, id);
            }
        }
        // Handle single participant (for other rules)
        else if (participantId != null) {
            participants.put(targetRole, participantId);
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
}