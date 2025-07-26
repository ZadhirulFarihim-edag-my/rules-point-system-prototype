package com.edag.swd.my.gamification;

import com.edag.swd.my.gamification.engine.RuleEngine;
import com.edag.swd.my.gamification.models.Event;
import com.edag.swd.my.gamification.models.Group;
import com.edag.swd.my.gamification.models.Person;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("normal-test")
public class SimulationRunner implements CommandLineRunner {

    private final RuleEngine ruleEngine;

    public SimulationRunner(RuleEngine ruleEngine) {
        this.ruleEngine = ruleEngine;
    }

    @Override
    public void run(String... args) throws Exception {
        // 1. Load rules from JSON
        ruleEngine.loadRules("/rules.json");

        // 2. Create groups and persons
        Group teamA = new Group("grp_a", "The Avengers");
        Group teamB = new Group("grp_b", "Justice League");
        Group teamC = new Group("grp_c", "Jujutsu Kaisen");

        Person alice = new Person("p_alice", "Alice", "grp_a");
        Person bob = new Person("p_bob", "Bob", "grp_a");
        Person charlie = new Person("p_charlie", "Charlie", "grp_a");
        Person max = new Person("p_diana", "Max", "grp_b");
        Person biagi = new Person("p_biagi", "Biagi", "grp_b");
        Person gojo = new Person("p_gojo", "Gojo", "grp_c");
        Person bestofrendo = new Person("p_bestofrendo", "BestoFrendo", "grp_c");

        ruleEngine.addGroup(teamA);
        ruleEngine.addGroup(teamB);
        ruleEngine.addGroup(teamC);
        ruleEngine.addPerson(alice);
        ruleEngine.addPerson(bob);
        ruleEngine.addPerson(charlie);
        ruleEngine.addPerson(max);
        ruleEngine.addPerson(biagi);
        ruleEngine.addPerson(gojo);
        ruleEngine.addPerson(bestofrendo);

        // --- Initial State ---
        System.out.println("🚀 SIMULATION STARTING... INITIAL STATE:");
        ruleEngine.printCurrentStateDetailed();

        // --- Simulate Events ---

        // Event 1: Simple award, no cap involved
        Event hackathonEvent = new Event("join_hackathon");
        hackathonEvent.addParticipant("participant", alice.getId());
        ruleEngine.processEvent(hackathonEvent);

        // Event 2: Test the merged SAP Hours processing
        System.out.println("\n--- TESTING MERGED SAP HOURS PROCESSING ---");
        System.out.println("Creating event with offenders: Biagi, Alice and Gojo");
        Event sapHoursEvent = new Event("did_not_key_in_sap_hour");
        sapHoursEvent.addParticipant("offender", biagi.getId());
        sapHoursEvent.addParticipant("offender", alice.getId());
        sapHoursEvent.addParticipant("offender", gojo.getId());
        ruleEngine.processEvent(sapHoursEvent);

        // Show state after first SAP Hours event
        System.out.println("\n--- STATE AFTER FIRST SAP HOURS EVENT ---");
        ruleEngine.printCurrentStateDetailed();

        // Event 3: Test another SAP Hours event with different offenders
        System.out.println("\n--- TESTING SECOND SAP HOURS EVENT ---");
        System.out.println("Creating event with offenders: Bestofrendo and Max");
        Event sapHoursEvent2 = new Event("did_not_key_in_sap_hour");
        sapHoursEvent2.addParticipant("offender", max.getId());
        sapHoursEvent2.addParticipant("offender", bestofrendo.getId());
        ruleEngine.processEvent(sapHoursEvent2);

        // Show state after second SAP Hours event
        System.out.println("\n--- STATE AFTER SECOND SAP HOURS EVENT ---");
        ruleEngine.printCurrentStateDetailed();

        // Event 3: Test another SAP Hours event with different offenders
        System.out.println("\n--- TESTING SECOND SAP HOURS EVENT ---");
        System.out.println("Creating event with offenders: Biagi");
        Event sapHoursEvent3 = new Event("did_not_key_in_sap_hour");
        sapHoursEvent3.addParticipant("offender", biagi.getId());
        ruleEngine.processEvent(sapHoursEvent3);

        // Show state after second SAP Hours event
        System.out.println("\n--- STATE AFTER SECOND SAP HOURS EVENT ---");
        ruleEngine.printCurrentStateDetailed();

        // Event 6: Winning a game
        Event winEvent = new Event("win_team_game");
        winEvent.addParticipant("winner", gojo.getId());
        ruleEngine.processEvent(winEvent);

        Event hackathonEvent2 = new Event("join_hackathon");
        hackathonEvent2.addParticipant("participant", gojo.getId());
        ruleEngine.processEvent(hackathonEvent2);

        // --- Final Summary ---
        ruleEngine.printSummary();
    }
}