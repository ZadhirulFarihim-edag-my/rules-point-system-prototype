package com.edag.swd.my.gamification.engine;

import com.edag.swd.my.gamification.config.OutcomeConfig;
import com.edag.swd.my.gamification.config.RuleConfig;
import com.edag.swd.my.gamification.models.Event;
import com.edag.swd.my.gamification.models.Group;
import com.edag.swd.my.gamification.models.Person;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class RuleEngine {
    private final Map<String, RuleConfig> rules = new ConcurrentHashMap<>();
    private final Map<String, Person> persons = new ConcurrentHashMap<>();
    private final Map<String, Group> groups = new ConcurrentHashMap<>();

    // Map to track the last reset time for each group's "sap_hours_compliant" rule
    // Key format: groupId + "_" + ruleName
    private final Map<String, Instant> lastResetTimeMap = new HashMap<>();

    // The rule name that should have weekly reset
    private static final String WEEKLY_RESET_RULE = "SAP Hours";

    public void loadRules(String resourcePath) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream inputStream = TypeReference.class.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IllegalArgumentException("Resource not found: " + resourcePath);
            }
            List<RuleConfig> ruleList = mapper.readValue(inputStream, new TypeReference<>() {
            });
            ruleList.forEach(rule -> this.rules.put(rule.getRuleName(), rule));
            System.out.println("Loaded " + ruleList.size() + " rules.");
        }
    }

    public void addPerson(Person person) {
        this.persons.put(person.getId(), person);
        if (this.groups.containsKey(person.getGroupId())) {
            this.groups.get(person.getGroupId()).addMember(person);
        }
    }

    public void addGroup(Group group) {
        this.groups.put(group.getId(), group);
    }

    public void processEvent(Event event) {
        System.out.println("\n-> Processing event: " + event.getActionType());

        // Special case for "did_not_key_in_sap_hour" event
        if ("did_not_key_in_sap_hour".equalsIgnoreCase(event.getActionType())) {
            processSapHoursEvent(event);
        } else {
            // Normal processing for other events
            this.rules.values().stream()
                    .filter(RuleConfig::isActive)
                    .filter(rule -> matches(rule, event))
                    .forEach(rule -> applyOutcomes(rule, event));
        }
    }

    private void processSapHoursEvent(Event event) {
        System.out.println("   - SPECIAL PROCESSING: Merged processing for SAP Hours");

        // Get the list of offenders from the event
        List<String> offenderIds = new ArrayList<>();
        Map<String, String> participants = event.getParticipants();

        for (Map.Entry<String, String> entry : participants.entrySet()) {
            if ("offender".equals(entry.getKey())) {
                String personId = entry.getValue();
                offenderIds.add(personId);

                // Apply penalty to offender's group
                Person offender = persons.get(personId);
                if (offender != null) {
                    Group group = groups.get(offender.getGroupId());
                    if (group != null) {
                        // Record the individual's contribution
                        offender.recordContribution(-2, "SAP Hours Offender in Group", WEEKLY_RESET_RULE);
                        System.out.printf("   - AUDIT: Recorded -2 points for %s due to '%s'.\n",
                                offender.getName(), WEEKLY_RESET_RULE);

                        // Update the group's total score
                        group.addPoints(-2, "SAP Hours Offender in Group", WEEKLY_RESET_RULE);
                        System.out.printf("   - ACTION: Group '%s' score changed by -2. New Total: %d.\n",
                                group.getName(), group.getTotalGroupPoints());
                    }
                }
            }
        }

        // Process each group to find non-offenders and reward them
        for (Group group : groups.values()) {
            // Get all members of the group who are not offenders
            List<Person> nonOffenders = new ArrayList<>();

            // Manually filter to find non-offenders
            for (Person person : persons.values()) {
                if (person.getGroupId().equals(group.getId()) && !offenderIds.contains(person.getId())) {
                    nonOffenders.add(person);
                }
            }

            if (!nonOffenders.isEmpty()) {
                System.out.printf("   - REWARD: Found %d non-offenders in group '%s'\n",
                        nonOffenders.size(), group.getName());

                // Get the rule for "SAP Hours Compliant"
                RuleConfig sapCompliantRule = this.rules.get(WEEKLY_RESET_RULE);
                if (sapCompliantRule != null && sapCompliantRule.getCap() != null) {
                    // Check if this is the rule that should have weekly reset
                    String resetKey = group.getId() + "_" + WEEKLY_RESET_RULE;
                    Instant now = Instant.now();
                    Instant lastResetTime = lastResetTimeMap.getOrDefault(resetKey, Instant.EPOCH);

                    // Check if a week (7 days) has passed since the last reset
                    if (Duration.between(lastResetTime, now).toDays() >= 7) {
                        // Reset the cap for this rule
                        group.resetActivityCap(WEEKLY_RESET_RULE);
                        System.out.printf("   - WEEKLY RESET: Resetting cap for rule '%s' for group '%s' as a week has passed.\n",
                                WEEKLY_RESET_RULE, group.getName());

                        // Update the last reset time
                        lastResetTimeMap.put(resetKey, now);
                    }

                    // Apply capping logic
                    int maxPoints = sapCompliantRule.getCap().getMaxPoints();
                    int currentPointsForActivity = group.getCurrentActivityPoints().getOrDefault(WEEKLY_RESET_RULE, 0);

                    // Calculate how many points to award (1 point per non-offender, up to the cap)
                    int pointsToAward = nonOffenders.size();

                    if (currentPointsForActivity >= maxPoints) {
                        pointsToAward = 0; // Cap already reached, no more points for the group
                    } else if (currentPointsForActivity + pointsToAward > maxPoints) {
                        pointsToAward = maxPoints - currentPointsForActivity; // Award partial points to hit the cap
                    }

                    // Update the group's tracking for this capped activity
                    group.getCurrentActivityPoints().put(WEEKLY_RESET_RULE, currentPointsForActivity + pointsToAward);
                    System.out.printf("   - CAPPING: Rule '%s' has a cap of %d. Group already has %d. Awarding %d points.\n",
                            WEEKLY_RESET_RULE, maxPoints, currentPointsForActivity, pointsToAward);

                    // Record contributions for each non-offender
                    for (Person nonOffender : nonOffenders) {
                        nonOffender.recordContribution(1, "All Group Members SAP Hours Compliant", WEEKLY_RESET_RULE);
                        System.out.printf("   - AUDIT: Recorded +1 point for %s due to '%s'.\n",
                                nonOffender.getName(), WEEKLY_RESET_RULE);
                    }

                    // Update the group's total score
                    if (pointsToAward > 0) {
                        group.addPoints(pointsToAward, "All Group Members SAP Hours Compliant", WEEKLY_RESET_RULE);
                        System.out.printf("   - ACTION: Group '%s' score changed by +%d. New Total: %d.\n",
                                group.getName(), pointsToAward, group.getTotalGroupPoints());
                    } else {
                        System.out.println("   - ACTION: No points awarded to group due to cap.");
                    }
                }
            }
        }
    }

    private boolean matches(RuleConfig rule, Event event) {
        return rule.getConditions().stream().allMatch(condition ->
                "action".equalsIgnoreCase(condition.getType()) &&
                        event.getActionType().equalsIgnoreCase(condition.getValue())
        );
    }

    private void applyOutcomes(RuleConfig rule, Event event) {
        for (OutcomeConfig outcome : rule.getOutcomes()) {
            String personId = event.getParticipants().get(outcome.getTarget());
            if (personId == null) continue;

            Person person = persons.get(personId);
            if (person == null) continue;

            Group group = groups.get(person.getGroupId());
            if (group == null) continue;

            // Always record the individual's contribution with the original point value for auditing
            person.recordContribution(outcome.getPoints(), outcome.getReason(), rule.getRuleName());
            System.out.printf("   - AUDIT: Recorded %d points for %s due to '%s'.\n",
                    outcome.getPoints(), person.getName(), rule.getRuleName());

            int pointsForGroup = outcome.getPoints();

            // Apply capping logic ONLY for awards, not penalties
            if ("award".equalsIgnoreCase(outcome.getType()) && rule.getCap() != null) {
                // Check if this is the rule that should have weekly reset
                if (WEEKLY_RESET_RULE.equals(rule.getRuleName())) {
                    String resetKey = group.getId() + "_" + rule.getRuleName();
                    Instant now = Instant.now();
                    Instant lastResetTime = lastResetTimeMap.getOrDefault(resetKey, Instant.EPOCH);

                    // Check if a week (7 days) has passed since the last reset
                    if (Duration.between(lastResetTime, now).toDays() >= 7) {
                        // Reset the cap for this rule
                        group.resetActivityCap(rule.getRuleName());
                        System.out.printf("   - WEEKLY RESET: Resetting cap for rule '%s' for group '%s' as a week has passed.\n",
                                rule.getRuleName(), group.getName());

                        // Update the last reset time
                        lastResetTimeMap.put(resetKey, now);
                    }
                }

                int maxPoints = rule.getCap().getMaxPoints();
                int currentPointsForActivity = group.getCurrentActivityPoints().getOrDefault(rule.getRuleName(), 0);

                if (currentPointsForActivity >= maxPoints) {
                    pointsForGroup = 0; // Cap already reached, no more points for the group
                } else if (currentPointsForActivity + pointsForGroup > maxPoints) {
                    pointsForGroup = maxPoints - currentPointsForActivity; // Award partial points to hit the cap
                }

                // Update the group's tracking for this capped activity
                group.getCurrentActivityPoints().put(rule.getRuleName(), currentPointsForActivity + pointsForGroup);
                System.out.printf("   - CAPPING: Rule '%s' has a cap of %d. Group already has %d. Awarding %d points.\n",
                        rule.getRuleName(), maxPoints, currentPointsForActivity, pointsForGroup);
            }

            // Update the group's total score
            group.addPoints(pointsForGroup, outcome.getReason(), rule.getRuleName());
            if (pointsForGroup != 0) {
                System.out.printf("   - ACTION: Group '%s' score changed by %d. New Total: %d.\n",
                        group.getName(), pointsForGroup, group.getTotalGroupPoints());
            } else {
                System.out.println("   - ACTION: No points awarded to group due to cap.");
            }
        }
    }

    public void printCurrentStateDetailed() {
        System.out.println("\n====================== DETAILED CURRENT STATE ======================");
        groups.values().forEach(group -> {
            System.out.printf("\n--- Group: %s (ID: %s) | Total Points: %d ---\n", group.getName(), group.getId(), group.getTotalGroupPoints());
            System.out.println("   Capped Activity Points: " + group.getCurrentActivityPoints());
            persons.values().stream()
                    .filter(p -> p.getGroupId().equals(group.getId()))
                    .forEach(person -> {
                        System.out.printf("   -> Person: %s (ID: %s)\n", person.getName(), person.getId());
                        person.getPointHistory().forEach(entry ->
                                System.out.printf("      - [%s] %s: %+d points\n", entry.timestamp().toString().substring(0, 19), entry.reason(), entry.pointsValue())
                        );
                        if (person.getPointHistory().isEmpty()) {
                            System.out.println("      - No history yet.");
                        }
                    });
        });
        System.out.println("==================================================================\n");
    }

    public void printSummary() {
        System.out.println("\n######################## FINAL SUMMARY #########################");

        System.out.println("\n## Individual Contribution History ##");
        persons.values().stream()
                .sorted(Comparator.comparing(Person::getName))
                .forEach(person -> {
                    String groupName = groups.get(person.getGroupId()).getName();
                    String history = person.getPointHistory().stream()
                            .map(entry -> String.format("%s: %+d", entry.reason(), entry.pointsValue()))
                            .collect(Collectors.joining(", "));
                    System.out.printf("- %s (%s): %s\n", person.getName(), groupName, history.isEmpty() ? "No contributions" : history);
                });

        System.out.println("\n## Team Standings ##");
        List<Group> sortedGroups = new ArrayList<>(groups.values());
        sortedGroups.sort(Comparator.comparingInt(Group::getTotalGroupPoints).reversed());

        int rank = 1;
        for (Group group : sortedGroups) {
            System.out.printf("%d. %s - %d points\n", rank++, group.getName(), group.getTotalGroupPoints());
        }
        System.out.println("############################################################\n");
    }
}