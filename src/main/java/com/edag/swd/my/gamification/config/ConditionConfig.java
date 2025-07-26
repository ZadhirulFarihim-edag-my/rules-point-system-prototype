package com.edag.swd.my.gamification.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConditionConfig {
    private String type;
    private String value;
}
