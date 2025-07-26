package com.edag.swd.my.gamification.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RuleConfig {
    private String ruleName;
    private String description;
    private boolean active;
    private boolean groupBasedActivity;
    private List<ConditionConfig> conditions;
    private List<OutcomeConfig> outcomes;
    private CapConfig cap;
}