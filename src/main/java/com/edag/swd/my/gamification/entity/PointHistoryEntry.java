package com.edag.swd.my.gamification.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "point_history")
@Data
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
public class PointHistoryEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int pointsValue;
    private String reason;
    private String ruleName;
    private Instant timestamp = Instant.now();

    @ManyToOne
    @JoinColumn(name = "person_id")
    private Person person;

    // Constructor for convenience
    public PointHistoryEntry(int pointsValue, String reason, String ruleName, Person person) {
        this.pointsValue = pointsValue;
        this.reason = reason;
        this.ruleName = ruleName;
        this.person = person;
        this.timestamp = Instant.now();
    }
}