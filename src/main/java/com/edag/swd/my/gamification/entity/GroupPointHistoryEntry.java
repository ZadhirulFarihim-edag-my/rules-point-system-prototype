package com.edag.swd.my.gamification.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "group_point_history")
@Data
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
public class GroupPointHistoryEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int pointsChanged;
    private String reason;
    private String ruleName;
    private Instant timestamp = Instant.now();

    @ManyToOne
    @JoinColumn(name = "group_id")
    private Group group;

    // Constructor for convenience
    public GroupPointHistoryEntry(int pointsChanged, String reason, String ruleName, Group group) {
        this.pointsChanged = pointsChanged;
        this.reason = reason;
        this.ruleName = ruleName;
        this.group = group;
        this.timestamp = Instant.now();
    }
}