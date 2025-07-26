package com.edag.swd.my.gamification.models;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Data
@RequiredArgsConstructor
public class Event {
    private final String id = UUID.randomUUID().toString();
    private final String actionType;
    private final Instant timestamp = Instant.now();
    private Map<String, String> participants = new HashMap<>();

    public void addParticipant(String role, String personId) {
        this.participants.put(role, personId);
    }
}
