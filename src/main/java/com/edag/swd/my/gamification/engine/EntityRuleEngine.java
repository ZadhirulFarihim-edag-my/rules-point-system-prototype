package com.edag.swd.my.gamification.engine;

import com.edag.swd.my.gamification.config.OutcomeConfig;
import com.edag.swd.my.gamification.config.RuleConfig;
import com.edag.swd.my.gamification.entity.Group;
import com.edag.swd.my.gamification.entity.Person;
import com.edag.swd.my.gamification.repository.GroupRepository;
import com.edag.swd.my.gamification.repository.PersonRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * EntityRuleEngine is a version of RuleEngine that works with entity classes and repositories
 * instead of model classes and in-memory collections.
 */
@Service
public class EntityRuleEngine {
    private final Map<String, RuleConfig> rules = new ConcurrentHashMap<>();
    private final PersonRepository personRepository;
    private final GroupRepository groupRepository;

    // Map to track the last reset time for each group's "sap_hours_compliant" rule
    // Key format: groupId + "_" + ruleName
    private final Map<String, Instant> lastResetTimeMap = new HashMap<>();

    // The rule name that should have weekly reset
    private static final String WEEKLY_RESET_RULE = "SAP Hours";

    // Path to the rules.json file
    private static final String RULES_JSON_PATH = "/rules.json";

    @Autowired
    public EntityRuleEngine(PersonRepository personRepository, GroupRepository groupRepository) {
        this.personRepository = personRepository;
        this.groupRepository = groupRepository;
    }

    /**
     * Ensures that rules are loaded before processing any rule-related request.
     * If rules are not loaded, loads them from rules.json.
     */
    private void ensureRulesLoaded() {
        if (rules.isEmpty()) {
            try {
                loadRules(RULES_JSON_PATH);
            } catch (Exception e) {
                System.err.println("Error loading rules: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Loads rules from a JSON file.
     *
     * @param resourcePath Path to the JSON file containing rules
     * @throws Exception If there's an error loading the rules
     */
    public void loadRules(String resourcePath) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream inputStream = TypeReference.class.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IllegalArgumentException("Resource not found: " + resourcePath);
            }
            List<RuleConfig> ruleList = mapper.readValue(inputStream, new TypeReference<>() {
            });
            ruleList.forEach(rule -> this.rules.put(rule.getRuleName(), rule));
            System.out.println("✅ Loaded " + ruleList.size() + " rules.");
        }
    }

    /**
     * Processes an event with the given action type and participants.
     *
     * @param actionType   The type of action to process
     * @param participants Map of participant roles to person IDs
     */
    @Transactional
    public void processEvent(String actionType, Map<String, String> participants) {
        // Ensure rules are loaded before processing the event
        ensureRulesLoaded();

        System.out.println("\n-> Processing event: " + actionType);

        // Special case for "did_not_key_in_sap_hour" event
        if ("did_not_key_in_sap_hour".equalsIgnoreCase(actionType)) {
            processSapHoursEvent(participants);
        } else {
            // Normal processing for other events
            this.rules.values().stream()
                    .filter(RuleConfig::isActive)
                    .filter(rule -> matches(rule, actionType))
                    .forEach(rule -> applyOutcomes(rule, participants));
        }
    }

    /**
     * Checks if a rule matches the given action type.
     *
     * @param rule       The rule to check
     * @param actionType The action type to match
     * @return true if the rule matches the action type, false otherwise
     */
    private boolean matches(RuleConfig rule, String actionType) {
        return rule.getConditions().stream().allMatch(condition ->
                "action".equalsIgnoreCase(condition.getType()) &&
                        actionType.equalsIgnoreCase(condition.getValue())
        );
    }

    /**
     * Applies the outcomes of a rule to the participants.
     *
     * @param rule         The rule to apply
     * @param participants Map of participant roles to person IDs
     */
    @Transactional
    private void applyOutcomes(RuleConfig rule, Map<String, String> participants) {
        for (OutcomeConfig outcome : rule.getOutcomes()) {
            String personId = participants.get(outcome.getTarget());
            if (personId == null) continue;

            Optional<Person> optionalPerson = personRepository.findById(personId);
            if (optionalPerson.isEmpty()) continue;

            Person person = optionalPerson.get();
            String groupId = person.getGroupId();
            Optional<Group> optionalGroup = groupRepository.findById(groupId);
            if (optionalGroup.isEmpty()) continue;

            Group group = optionalGroup.get();

            // Record the individual's contribution
            person.recordContribution(outcome.getPoints(), outcome.getReason(), rule.getRuleName());
            personRepository.save(person);
            System.out.printf("   - AUDIT: Recorded %+d points for %s due to '%s'.\n",
                    outcome.getPoints(), person.getName(), rule.getRuleName());

            // Apply capping logic ONLY for awards, not penalties
            if ("award".equalsIgnoreCase(outcome.getType()) && rule.getCap() != null) {
                // Check if this is the rule that should have weekly reset
                if (WEEKLY_RESET_RULE.equals(rule.getRuleName())) {
                    String resetKey = groupId + "_" + rule.getRuleName();
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
                int currentPointsForActivity = group.getCurrentPointsForActivity(rule.getRuleName());

                // Calculate how many points to award (up to the cap)
                int pointsToAward = outcome.getPoints();

                if (currentPointsForActivity >= maxPoints) {
                    pointsToAward = 0; // Cap already reached, no more points
                    System.out.printf("   - CAPPING: Rule '%s' has a cap of %d. Group already has %d. No points awarded.\n",
                            rule.getRuleName(), maxPoints, currentPointsForActivity);
                } else if (currentPointsForActivity + pointsToAward > maxPoints) {
                    pointsToAward = maxPoints - currentPointsForActivity; // Award partial points to hit the cap
                    System.out.printf("   - CAPPING: Rule '%s' has a cap of %d. Group already has %d. Awarding %d points (partial).\n",
                            rule.getRuleName(), maxPoints, currentPointsForActivity, pointsToAward);
                } else {
                    System.out.printf("   - CAPPING: Rule '%s' has a cap of %d. Group already has %d. Awarding %d points.\n",
                            rule.getRuleName(), maxPoints, currentPointsForActivity, pointsToAward);
                }

                // Update the group's tracking for this capped activity
                group.updateActivityPoints(rule.getRuleName(), currentPointsForActivity + pointsToAward);

                // Update the group's total score
                if (pointsToAward > 0) {
                    group.addPoints(pointsToAward, outcome.getReason(), rule.getRuleName());
                    groupRepository.save(group);
                    System.out.printf("   - ACTION: Group '%s' score changed by %+d. New Total: %d.\n",
                            group.getName(), pointsToAward, group.getTotalGroupPoints());
                }
            } else {
                // For penalties or uncapped awards, simply add the points
                group.addPoints(outcome.getPoints(), outcome.getReason(), rule.getRuleName());
                groupRepository.save(group);
                System.out.printf("   - ACTION: Group '%s' score changed by %+d. New Total: %d.\n",
                        group.getName(), outcome.getPoints(), group.getTotalGroupPoints());
            }
        }
    }

    /**
     * Processes a SAP Hours event with special logic for offenders and non-offenders.
     *
     * @param participants Map of participant roles to person IDs
     */
    @Transactional
    private void processSapHoursEvent(Map<String, String> participants) {
        System.out.println("   - SPECIAL PROCESSING: Merged processing for SAP Hours");

        // Get the list of offenders from the event
        List<String> offenderIds = new ArrayList<>();

        for (Map.Entry<String, String> entry : participants.entrySet()) {
            if ("offender".equals(entry.getKey())) {
                String personId = entry.getValue();
                offenderIds.add(personId);

                // Apply penalty to offender's group
                Optional<Person> optionalOffender = personRepository.findById(personId);
                if (optionalOffender.isPresent()) {
                    Person offender = optionalOffender.get();
                    String groupId = offender.getGroupId();
                    Optional<Group> optionalGroup = groupRepository.findById(groupId);

                    if (optionalGroup.isPresent()) {
                        Group group = optionalGroup.get();

                        // Record the individual's contribution
                        offender.recordContribution(-2, "SAP Hours Offender in Group", WEEKLY_RESET_RULE);
                        personRepository.save(offender);
                        System.out.printf("   - AUDIT: Recorded -2 points for %s due to '%s'.\n",
                                offender.getName(), WEEKLY_RESET_RULE);

                        // Update the group's total score
                        group.addPoints(-2, "SAP Hours Offender in Group", WEEKLY_RESET_RULE);
                        groupRepository.save(group);
                        System.out.printf("   - ACTION: Group '%s' score changed by -2. New Total: %d.\n",
                                group.getName(), group.getTotalGroupPoints());
                    }
                }
            }
        }

        // Process each group to find non-offenders and reward them
        List<Group> allGroups = groupRepository.findAll();
        for (Group group : allGroups) {
            // Get all members of the group who are not offenders
            List<Person> groupMembers = personRepository.findByGroupId(group.getId());
            List<Person> nonOffenders = groupMembers.stream()
                    .filter(person -> !offenderIds.contains(person.getId()))
                    .toList();

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
                    int currentPointsForActivity = group.getCurrentPointsForActivity(WEEKLY_RESET_RULE);

                    // Calculate how many points to award (1 point per non-offender, up to the cap)
                    int pointsToAward = nonOffenders.size();

                    if (currentPointsForActivity >= maxPoints) {
                        pointsToAward = 0; // Cap already reached, no more points for the group
                        continue;
                    } else if (currentPointsForActivity + pointsToAward > maxPoints) {
                        pointsToAward = maxPoints - currentPointsForActivity; // Award partial points to hit the cap
                    }

                    // Update the group's tracking for this capped activity
                    group.updateActivityPoints(WEEKLY_RESET_RULE, currentPointsForActivity + pointsToAward);
                    System.out.printf("   - CAPPING: Rule '%s' has a cap of %d. Group already has %d. Awarding %d points.\n",
                            WEEKLY_RESET_RULE, maxPoints, currentPointsForActivity, pointsToAward);

                    // Record contributions for each non-offender
                    for (Person nonOffender : nonOffenders) {
                        nonOffender.recordContribution(1, "All Group Members SAP Hours Compliant", WEEKLY_RESET_RULE);
                        personRepository.save(nonOffender);
                        System.out.printf("   - AUDIT: Recorded +1 point for %s due to '%s'.\n",
                                nonOffender.getName(), WEEKLY_RESET_RULE);
                    }

                    // Update the group's total score
                    if (pointsToAward > 0) {
                        group.addPoints(pointsToAward, "All Group Members SAP Hours Compliant", WEEKLY_RESET_RULE);
                        groupRepository.save(group);
                        System.out.printf("   - ACTION: Group '%s' score changed by +%d. New Total: %d.\n",
                                group.getName(), pointsToAward, group.getTotalGroupPoints());
                    } else {
                        System.out.println("   - ACTION: No points awarded to group due to cap.");
                    }
                }
            }
        }
    }

    /**
     * Gets the loaded rules.
     *
     * @return Map of rule names to rule configurations
     */
    public Map<String, RuleConfig> getRules() {
        // Ensure rules are loaded before returning them
        ensureRulesLoaded();
        return Collections.unmodifiableMap(this.rules);
    }
}