package com.edag.swd.my.gamification.models;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(of = "id")
@ToString(exclude = "pointHistory")
public class Person {
    private final String id;
    private final String name;
    private final String groupId;
    private List<PointHistoryEntry> pointHistory = new ArrayList<>();
    private int totalAccumulatedPoints = 0;

    public void recordContribution(int pointsValue, String reason, String ruleName) {
        this.pointHistory.add(new PointHistoryEntry(pointsValue, reason, ruleName, Instant.now()));
    }

    public record PointHistoryEntry(int pointsValue, String reason, String ruleName, Instant timestamp) {
    }
}
