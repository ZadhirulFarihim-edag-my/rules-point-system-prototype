package com.edag.swd.my.gamification.controller;

import com.edag.swd.my.gamification.service.RuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Global controller advice to add common attributes to all models.
 * This makes certain data available to all templates without having to add it in each controller method.
 */
@ControllerAdvice
public class GlobalControllerAdvice {

    private final RuleService ruleService;

    @Autowired
    public GlobalControllerAdvice(RuleService ruleService) {
        this.ruleService = ruleService;
    }

    /**
     * Add all rules to the model for all requests.
     * This makes the rules available to all templates, including layouts.
     */
    @ModelAttribute
    public void addGlobalAttributes(Model model) {
        model.addAttribute("allRules", ruleService.getRules().values());
    }
}