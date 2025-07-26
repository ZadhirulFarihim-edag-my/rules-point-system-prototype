package com.edag.swd.my.gamification.repository;

import com.edag.swd.my.gamification.entity.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupRepository extends JpaRepository<Group, String> {
    // Find a group by name
    Group findByName(String name);

    // Find groups by name containing a specific string (case insensitive)
    List<Group> findByNameContainingIgnoreCase(String nameFragment);

    // Find groups with total points greater than a specific value
    List<Group> findByTotalGroupPointsGreaterThan(int points);

    // Find groups with total points less than a specific value
    List<Group> findByTotalGroupPointsLessThan(int points);
}