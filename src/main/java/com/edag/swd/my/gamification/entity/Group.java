package com.edag.swd.my.gamification.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "`groups`")
@Data
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"members", "groupPointHistory", "activityPoints"})
public class Group {
    @Id
    private String id;

    private String name;

    private int totalGroupPoints = 0;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Person> members = new HashSet<>();

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GroupPointHistoryEntry> groupPointHistory = new ArrayList<>();

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ActivityPoints> activityPoints = new ArrayList<>();

    // Constructor with required fields
    public Group(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public void addMember(Person person) {
        this.members.add(person);
        person.setGroup(this);
    }

    public void addPoints(int pointsToAdd, String reason, String ruleName) {
        if (pointsToAdd != 0) {
            this.totalGroupPoints += pointsToAdd;

            GroupPointHistoryEntry entry = new GroupPointHistoryEntry();
            entry.setGroup(this);
            entry.setPointsChanged(pointsToAdd);
            entry.setReason(reason);
            entry.setRuleName(ruleName);

            this.groupPointHistory.add(entry);
        }
    }

    public void resetActivityCap(String ruleName) {
        // Find and remove the activity points for the given rule
        this.activityPoints.removeIf(ap -> ap.getRuleName().equals(ruleName));
    }

    // Helper method to get current points for an activity
    public int getCurrentPointsForActivity(String ruleName) {
        return this.activityPoints.stream()
                .filter(ap -> ap.getRuleName().equals(ruleName))
                .findFirst()
                .map(ActivityPoints::getPoints)
                .orElse(0);
    }

    // Helper method to update points for an activity
    public void updateActivityPoints(String ruleName, int points) {
        ActivityPoints activityPoint = this.activityPoints.stream()
                .filter(ap -> ap.getRuleName().equals(ruleName))
                .findFirst()
                .orElse(null);

        if (activityPoint == null) {
            activityPoint = new ActivityPoints();
            activityPoint.setGroup(this);
            activityPoint.setRuleName(ruleName);
            activityPoint.setPoints(points);
            this.activityPoints.add(activityPoint);
        } else {
            activityPoint.setPoints(points);
        }
    }
}