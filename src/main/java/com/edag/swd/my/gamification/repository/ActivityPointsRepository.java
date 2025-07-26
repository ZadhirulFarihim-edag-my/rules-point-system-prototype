package com.edag.swd.my.gamification.repository;

import com.edag.swd.my.gamification.entity.ActivityPoints;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ActivityPointsRepository extends JpaRepository<ActivityPoints, Long> {
    // Find all activity points for a specific group
    List<ActivityPoints> findByGroupId(String groupId);

    // Find all activity points for a specific rule
    List<ActivityPoints> findByRuleName(String ruleName);

    // Find activity points for a specific group and rule
    Optional<ActivityPoints> findByGroupIdAndRuleName(String groupId, String ruleName);

    // Find all activity points with points greater than a specific value
    List<ActivityPoints> findByPointsGreaterThan(int points);

    // Find all activity points with points less than a specific value
    List<ActivityPoints> findByPointsLessThan(int points);

    // Delete all activity points for a specific group and rule
    void deleteByGroupIdAndRuleName(String groupId, String ruleName);
}