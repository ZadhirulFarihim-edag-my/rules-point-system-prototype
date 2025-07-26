package com.edag.swd.my.gamification.models;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.Instant;
import java.util.*;

@Data
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"members", "groupPointHistory"})
public class Group {
    private final String id;
    private final String name;
    private Set<Person> members = new HashSet<>();
    private int totalGroupPoints = 0;
    private List<GroupPointHistoryEntry> groupPointHistory = new ArrayList<>();

    // Tracks points awarded for a specific rule to enforce capping
    private Map<String, Integer> currentActivityPoints = new HashMap<>();

    public void addMember(Person person) {
        this.members.add(person);
    }

    public void addPoints(int pointsToAdd, String reason, String ruleName) {
        if (pointsToAdd != 0) {
            this.totalGroupPoints += pointsToAdd;
            this.groupPointHistory.add(new GroupPointHistoryEntry(pointsToAdd, reason, ruleName, Instant.now()));
        }
    }

    public void resetActivityCap(String ruleName) {
        this.currentActivityPoints.remove(ruleName);
    }

    public record GroupPointHistoryEntry(int pointsChanged, String reason, String ruleName, Instant timestamp) {

    }
}