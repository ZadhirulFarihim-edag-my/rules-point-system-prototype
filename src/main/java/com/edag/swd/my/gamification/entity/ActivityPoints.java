package com.edag.swd.my.gamification.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "activity_points")
@Data
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
public class ActivityPoints {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String ruleName;
    private int points;

    @ManyToOne
    @JoinColumn(name = "group_id")
    private Group group;

    // Constructor for convenience
    public ActivityPoints(String ruleName, int points, Group group) {
        this.ruleName = ruleName;
        this.points = points;
        this.group = group;
    }
}