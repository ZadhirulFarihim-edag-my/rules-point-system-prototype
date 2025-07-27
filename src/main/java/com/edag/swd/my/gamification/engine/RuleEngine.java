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
public class RuleEngine {
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
    public RuleEngine(PersonRepository personRepository, GroupRepository groupRepository) {
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

        // Process all matching rules
        this.rules.values().stream()
                .filter(RuleConfig::isActive)
                .filter(rule -> matches(rule, actionType))
                .forEach(rule -> {
                    // Check if the rule has multiple outcome types (both award and penalty)
                    if (hasMultipleOutcomeTypes(rule)) {
                        // Use the generic method for rules with multiple outcome types
                        processMultiOutcomeRule(rule, participants);
                    } else {
                        // Use the standard method for rules with a single outcome type
                        applyOutcomes(rule, participants);
                    }
                });
    }

    /**
     * Checks if a rule has multiple outcome types (both award and penalty).
     *
     * @param rule The rule to check
     * @return true if the rule has both award and penalty outcomes, false otherwise
     */
    private boolean hasMultipleOutcomeTypes(RuleConfig rule) {
        boolean hasAward = false;
        boolean hasPenalty = false;

        for (OutcomeConfig outcome : rule.getOutcomes()) {
            if ("award".equalsIgnoreCase(outcome.getType())) {
                hasAward = true;
            } else if ("penalty".equalsIgnoreCase(outcome.getType())) {
                hasPenalty = true;
            }

            // If we've found both types, we can return early
            if (hasAward && hasPenalty) {
                return true;
            }
        }

        return false;
    }

    /**
     * Processes a rule with multiple outcome types (both award and penalty).
     * This is a generic implementation that can handle any rule with multiple outcome types,
     * not just the SAP Hours rule.
     *
     * @param rule         The rule to process
     * @param participants Map of participant roles to person IDs
     */
    @Transactional
    private void processMultiOutcomeRule(RuleConfig rule, Map<String, String> participants) {
        System.out.println("   - SPECIAL PROCESSING: Generic processing for rule with multiple outcome types: " + rule.getRuleName());

        // Get all penalty targets from the rule's outcomes
        List<String> penaltyTargets = rule.getOutcomes().stream()
                .filter(outcome -> "penalty".equalsIgnoreCase(outcome.getType()))
                .map(OutcomeConfig::getTarget)
                .distinct()
                .toList();

        // Get all award targets from the rule's outcomes
        List<String> awardTargets = rule.getOutcomes().stream()
                .filter(outcome -> "award".equalsIgnoreCase(outcome.getType()))
                .map(OutcomeConfig::getTarget)
                .distinct()
                .toList();

        // Process penalties first
        List<String> penalizedPersonIds = new ArrayList<>();

        // Apply penalties to all penalty targets
        for (String penaltyTarget : penaltyTargets) {
            // Get all persons with this target role
            for (Map.Entry<String, String> entry : participants.entrySet()) {
                if (penaltyTarget.equals(entry.getKey())) {
                    String personId = entry.getValue();
                    penalizedPersonIds.add(personId);

                    // Find the penalty outcome for this target
                    OutcomeConfig penaltyOutcome = rule.getOutcomes().stream()
                            .filter(outcome -> "penalty".equalsIgnoreCase(outcome.getType()) &&
                                    penaltyTarget.equals(outcome.getTarget()))
                            .findFirst()
                            .orElse(null);

                    if (penaltyOutcome == null) continue;

                    // Apply penalty to the person and their group
                    Optional<Person> optionalPerson = personRepository.findById(personId);
                    if (optionalPerson.isEmpty()) continue;

                    Person person = optionalPerson.get();
                    String groupId = person.getGroupId();
                    Optional<Group> optionalGroup = groupRepository.findById(groupId);
                    if (optionalGroup.isEmpty()) continue;

                    Group group = optionalGroup.get();

                    // Record the individual's contribution
                    person.recordContribution(penaltyOutcome.getPoints(), penaltyOutcome.getReason(), rule.getRuleName());
                    personRepository.save(person);
                    System.out.printf("   - AUDIT: Recorded %+d points for %s due to '%s'.\n",
                            penaltyOutcome.getPoints(), person.getName(), rule.getRuleName());

                    // Update the group's total score
                    group.addPoints(penaltyOutcome.getPoints(), penaltyOutcome.getReason(), rule.getRuleName());
                    groupRepository.save(group);
                    System.out.printf("   - ACTION: Group '%s' score changed by %+d. New Total: %d.\n",
                            group.getName(), penaltyOutcome.getPoints(), group.getTotalGroupPoints());
                }
            }
        }

        // Process awards for all groups
        List<Group> allGroups = groupRepository.findAll();
        for (Group group : allGroups) {
            // For each award target, find all eligible persons in the group
            for (String awardTarget : awardTargets) {
                List<Person> groupMembers = personRepository.findByGroupId(group.getId());
                List<Person> eligiblePersons;

                // Special handling for "compliant" target
                if ("compliant".equals(awardTarget)) {
                    // Check if there are specific compliant participants in the participants map
                    boolean hasSpecificCompliantParticipants = participants.entrySet().stream()
                            .anyMatch(entry -> "compliant".equals(entry.getKey()));

                    if (hasSpecificCompliantParticipants) {
                        // Use the specified compliant participants
                        eligiblePersons = new ArrayList<>();
                        for (Map.Entry<String, String> entry : participants.entrySet()) {
                            if ("compliant".equals(entry.getKey())) {
                                String personId = entry.getValue();
                                Optional<Person> optionalPerson = personRepository.findById(personId);
                                if (optionalPerson.isPresent() &&
                                        group.getId().equals(optionalPerson.get().getGroupId())) {
                                    eligiblePersons.add(optionalPerson.get());
                                }
                            }
                        }
                        System.out.println("   - SPECIAL HANDLING: Using specified compliant participants for rule: " + rule.getRuleName());
                    } else {
                        // Default behavior: all persons who are not in the penalized list
                        eligiblePersons = groupMembers.stream()
                                .filter(person -> !penalizedPersonIds.contains(person.getId()))
                                .toList();
                        System.out.println("   - DEFAULT HANDLING: Using all non-penalized persons as compliant for rule: " + rule.getRuleName());
                    }
                } else {
                    // For other targets, check if they are in the participants map
                    eligiblePersons = new ArrayList<>();
                    for (Map.Entry<String, String> entry : participants.entrySet()) {
                        if (awardTarget.equals(entry.getKey())) {
                            String personId = entry.getValue();
                            Optional<Person> optionalPerson = personRepository.findById(personId);
                            if (optionalPerson.isPresent() &&
                                    group.getId().equals(optionalPerson.get().getGroupId())) {
                                eligiblePersons.add(optionalPerson.get());
                            }
                        }
                    }
                }

                if (eligiblePersons.isEmpty()) continue;

                System.out.printf("   - REWARD: Found %d eligible persons for target '%s' in group '%s'\n",
                        eligiblePersons.size(), awardTarget, group.getName());

                // Find the award outcome for this target
                OutcomeConfig awardOutcome = rule.getOutcomes().stream()
                        .filter(outcome -> "award".equalsIgnoreCase(outcome.getType()) &&
                                awardTarget.equals(outcome.getTarget()))
                        .findFirst()
                        .orElse(null);

                if (awardOutcome == null) continue;

                // Apply capping logic if the rule has a cap
                if (rule.getCap() != null) {
                    // Check if this rule needs a weekly reset
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
                    int currentPointsForActivity = group.getCurrentPointsForActivity(rule.getRuleName());

                    // Calculate how many points to award (1 point per eligible person, up to the cap)
                    int pointsToAward = eligiblePersons.size() * awardOutcome.getPoints();

                    if (currentPointsForActivity >= maxPoints) {
                        pointsToAward = 0; // Cap already reached, no more points for the group
                        System.out.printf("   - CAPPING: Rule '%s' has a cap of %d. Group already has %d. No points awarded.\n",
                                rule.getRuleName(), maxPoints, currentPointsForActivity);
                        continue;
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

                    // Record contributions for each eligible person
                    for (Person eligiblePerson : eligiblePersons) {
                        eligiblePerson.recordContribution(awardOutcome.getPoints(), awardOutcome.getReason(), rule.getRuleName());
                        personRepository.save(eligiblePerson);
                        System.out.printf("   - AUDIT: Recorded %+d point for %s due to '%s'.\n",
                                awardOutcome.getPoints(), eligiblePerson.getName(), rule.getRuleName());
                    }

                    // Update the group's total score
                    if (pointsToAward > 0) {
                        group.addPoints(pointsToAward, awardOutcome.getReason(), rule.getRuleName());
                        groupRepository.save(group);
                        System.out.printf("   - ACTION: Group '%s' score changed by %+d. New Total: %d.\n",
                                group.getName(), pointsToAward, group.getTotalGroupPoints());
                    } else {
                        System.out.println("   - ACTION: No points awarded to group due to cap.");
                    }
                } else {
                    // For uncapped awards, simply add the points for each eligible person
                    int totalPoints = eligiblePersons.size() * awardOutcome.getPoints();

                    // Record contributions for each eligible person
                    for (Person eligiblePerson : eligiblePersons) {
                        eligiblePerson.recordContribution(awardOutcome.getPoints(), awardOutcome.getReason(), rule.getRuleName());
                        personRepository.save(eligiblePerson);
                        System.out.printf("   - AUDIT: Recorded %+d point for %s due to '%s'.\n",
                                awardOutcome.getPoints(), eligiblePerson.getName(), rule.getRuleName());
                    }

                    // Update the group's total score
                    group.addPoints(totalPoints, awardOutcome.getReason(), rule.getRuleName());
                    groupRepository.save(group);
                    System.out.printf("   - ACTION: Group '%s' score changed by %+d. New Total: %d.\n",
                            group.getName(), totalPoints, group.getTotalGroupPoints());
                }
            }
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

            // Check if this is an award outcome
            if ("award".equalsIgnoreCase(outcome.getType())) {
                // Apply capping logic if the rule has a cap
                if (rule.getCap() != null) {
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
                    // For uncapped awards, simply add the points
                    System.out.println("   - UNCAPPED AWARD: Adding " + outcome.getPoints() + " points for rule: " + rule.getRuleName());
                    group.addPoints(outcome.getPoints(), outcome.getReason(), rule.getRuleName());
                    groupRepository.save(group);
                    System.out.printf("   - ACTION: Group '%s' score changed by %+d. New Total: %d.\n",
                            group.getName(), outcome.getPoints(), group.getTotalGroupPoints());
                }
            } else {
                // For penalties, simply add the points (which will be negative)
                group.addPoints(outcome.getPoints(), outcome.getReason(), rule.getRuleName());
                groupRepository.save(group);
                System.out.printf("   - ACTION: Group '%s' score changed by %+d. New Total: %d.\n",
                        group.getName(), outcome.getPoints(), group.getTotalGroupPoints());
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