package com.edag.swd.my.gamification.service;

import com.edag.swd.my.gamification.entity.ActivityPoints;
import com.edag.swd.my.gamification.entity.Group;
import com.edag.swd.my.gamification.entity.GroupPointHistoryEntry;
import com.edag.swd.my.gamification.entity.Person;
import com.edag.swd.my.gamification.repository.ActivityPointsRepository;
import com.edag.swd.my.gamification.repository.GroupPointHistoryEntryRepository;
import com.edag.swd.my.gamification.repository.GroupRepository;
import com.edag.swd.my.gamification.repository.PersonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class GroupService {

    private final GroupRepository groupRepository;
    private final PersonRepository personRepository;
    private final GroupPointHistoryEntryRepository groupPointHistoryEntryRepository;
    private final ActivityPointsRepository activityPointsRepository;

    @Autowired
    public GroupService(GroupRepository groupRepository,
                        PersonRepository personRepository,
                        GroupPointHistoryEntryRepository groupPointHistoryEntryRepository,
                        ActivityPointsRepository activityPointsRepository) {
        this.groupRepository = groupRepository;
        this.personRepository = personRepository;
        this.groupPointHistoryEntryRepository = groupPointHistoryEntryRepository;
        this.activityPointsRepository = activityPointsRepository;
    }

    public List<Group> getAllGroups() {
        return groupRepository.findAll();
    }

    public Optional<Group> getGroupById(String id) {
        return groupRepository.findById(id);
    }

    public Group getGroupByName(String name) {
        return groupRepository.findByName(name);
    }

    public List<Group> searchGroupsByName(String nameFragment) {
        return groupRepository.findByNameContainingIgnoreCase(nameFragment);
    }

    @Transactional
    public Group saveGroup(Group group) {
        return groupRepository.save(group);
    }

    @Transactional
    public void deleteGroup(String id) {
        groupRepository.deleteById(id);
    }

    @Transactional
    public void addMemberToGroup(String groupId, String personId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found with ID: " + groupId));

        Person person = personRepository.findById(personId)
                .orElseThrow(() -> new IllegalArgumentException("Person not found with ID: " + personId));

        group.addMember(person);
        person.setGroup(group);

        groupRepository.save(group);
        personRepository.save(person);
    }

    @Transactional
    public void addPoints(String groupId, int pointsToAdd, String reason, String ruleName) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found with ID: " + groupId));

        if (pointsToAdd != 0) {
            // Update total points
            group.setTotalGroupPoints(group.getTotalGroupPoints() + pointsToAdd);

            // Create and save history entry
            GroupPointHistoryEntry entry = new GroupPointHistoryEntry();
            entry.setGroup(group);
            entry.setPointsChanged(pointsToAdd);
            entry.setReason(reason);
            entry.setRuleName(ruleName);

            groupPointHistoryEntryRepository.save(entry);
            group.getGroupPointHistory().add(entry);

            groupRepository.save(group);
        }
    }

    @Transactional
    public void updateActivityPoints(String groupId, String ruleName, int points) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found with ID: " + groupId));

        Optional<ActivityPoints> existingPoints = activityPointsRepository.findByGroupIdAndRuleName(groupId, ruleName);

        if (existingPoints.isPresent()) {
            ActivityPoints activityPoints = existingPoints.get();
            activityPoints.setPoints(points);
            activityPointsRepository.save(activityPoints);
        } else {
            ActivityPoints activityPoints = new ActivityPoints(ruleName, points, group);
            activityPointsRepository.save(activityPoints);
            group.getActivityPoints().add(activityPoints);
            groupRepository.save(group);
        }
    }

    @Transactional
    public void resetActivityCap(String groupId, String ruleName) {
        activityPointsRepository.deleteByGroupIdAndRuleName(groupId, ruleName);
    }

    public int getCurrentPointsForActivity(String groupId, String ruleName) {
        Optional<ActivityPoints> activityPoints = activityPointsRepository.findByGroupIdAndRuleName(groupId, ruleName);
        return activityPoints.map(ActivityPoints::getPoints).orElse(0);
    }

    public List<GroupPointHistoryEntry> getGroupPointHistory(String groupId) {
        return groupPointHistoryEntryRepository.findByGroupId(groupId);
    }
}