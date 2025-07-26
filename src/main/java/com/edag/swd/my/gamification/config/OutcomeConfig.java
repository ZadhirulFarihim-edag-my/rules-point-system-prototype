package com.edag.swd.my.gamification.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OutcomeConfig {
    private String type; // "award" or "penalty"
    private int points;
    private String target; // e.g., "offender", "compliant_user"
    private String reason;
}