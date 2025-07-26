package com.edag.swd.my.gamification.repository;

import com.edag.swd.my.gamification.entity.GroupPointHistoryEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface GroupPointHistoryEntryRepository extends JpaRepository<GroupPointHistoryEntry, Long> {
    // Find all group point history entries for a specific group
    List<GroupPointHistoryEntry> findByGroupId(String groupId);

    // Find all group point history entries for a specific rule
    List<GroupPointHistoryEntry> findByRuleName(String ruleName);

    // Find all group point history entries for a specific group and rule
    List<GroupPointHistoryEntry> findByGroupIdAndRuleName(String groupId, String ruleName);

    // Find all group point history entries after a specific timestamp
    List<GroupPointHistoryEntry> findByTimestampAfter(Instant timestamp);

    // Find all group point history entries between two timestamps
    List<GroupPointHistoryEntry> findByTimestampBetween(Instant startTime, Instant endTime);
}