package com.edag.swd.my.gamification.repository;

import com.edag.swd.my.gamification.entity.PointHistoryEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface PointHistoryEntryRepository extends JpaRepository<PointHistoryEntry, Long> {
    // Find all point history entries for a specific person
    @Query("SELECT phe FROM PointHistoryEntry phe WHERE phe.person.id = :personId")
    List<PointHistoryEntry> findByPersonId(String personId);

    // Find all point history entries for a specific rule
    List<PointHistoryEntry> findByRuleName(String ruleName);

    // Find all point history entries for a specific person and rule
    @Query("SELECT phe FROM PointHistoryEntry phe WHERE phe.person.id = :personId and phe.ruleName = :ruleName")
    List<PointHistoryEntry> findByPersonIdAndRuleName(String personId, String ruleName);

    // Find all point history entries after a specific timestamp
    List<PointHistoryEntry> findByTimestampAfter(Instant timestamp);

    // Find all point history entries between two timestamps
    List<PointHistoryEntry> findByTimestampBetween(Instant startTime, Instant endTime);
}