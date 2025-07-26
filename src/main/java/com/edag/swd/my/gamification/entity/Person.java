package com.edag.swd.my.gamification.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "persons")
@Data
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"pointHistory", "group"})
public class Person {
    @Id
    private String id;

    private String name;

    @ManyToOne
    @JoinColumn(name = "group_id")
    private Group group;

    @OneToMany(mappedBy = "person", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PointHistoryEntry> pointHistory = new ArrayList<>();


    // Constructor with required fields
    public Person(String id, String name, Group group) {
        this.id = id;
        this.name = name;
        this.group = group;
    }

    // Convenience constructor that takes groupId instead of Group
    public Person(String id, String name, String groupId) {
        this.id = id;
        this.name = name;
        // The group will be set later
    }

    public void recordContribution(int pointsValue, String reason, String ruleName) {
        PointHistoryEntry entry = new PointHistoryEntry();
        entry.setPerson(this);
        entry.setPointsValue(pointsValue);
        entry.setReason(reason);
        entry.setRuleName(ruleName);
        this.pointHistory.add(entry);
    }

    // Helper method to get the group ID
    public String getGroupId() {
        return group != null ? group.getId() : null;
    }

    // Calculate total accumulated points
    public int getTotalAccumulatedPoints() {
        return pointHistory.stream()
                .mapToInt(PointHistoryEntry::getPointsValue)
                .sum();
    }
}