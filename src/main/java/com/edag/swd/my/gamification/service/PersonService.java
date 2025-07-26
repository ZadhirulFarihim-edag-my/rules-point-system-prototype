package com.edag.swd.my.gamification.service;

import com.edag.swd.my.gamification.entity.Person;
import com.edag.swd.my.gamification.entity.PointHistoryEntry;
import com.edag.swd.my.gamification.repository.PersonRepository;
import com.edag.swd.my.gamification.repository.PointHistoryEntryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class PersonService {

    private final PersonRepository personRepository;
    private final PointHistoryEntryRepository pointHistoryEntryRepository;

    @Autowired
    public PersonService(PersonRepository personRepository, PointHistoryEntryRepository pointHistoryEntryRepository) {
        this.personRepository = personRepository;
        this.pointHistoryEntryRepository = pointHistoryEntryRepository;
    }

    public List<Person> getAllPersons() {
        return personRepository.findAll();
    }

    public Optional<Person> getPersonById(String id) {
        return personRepository.findById(id);
    }

    public Person getPersonByName(String name) {
        return personRepository.findByName(name);
    }

    public List<Person> getPersonsByGroupId(String groupId) {
        return personRepository.findByGroupId(groupId);
    }

    public List<Person> searchPersonsByName(String nameFragment) {
        return personRepository.findByNameContainingIgnoreCase(nameFragment);
    }

    @Transactional
    public Person savePerson(Person person) {
        return personRepository.save(person);
    }

    @Transactional
    public void deletePerson(String id) {
        personRepository.deleteById(id);
    }

    @Transactional
    public void recordContribution(String personId, int pointsValue, String reason, String ruleName) {
        Person person = personRepository.findById(personId)
                .orElseThrow(() -> new IllegalArgumentException("Person not found with ID: " + personId));

        PointHistoryEntry entry = new PointHistoryEntry();
        entry.setPerson(person);
        entry.setPointsValue(pointsValue);
        entry.setReason(reason);
        entry.setRuleName(ruleName);

        pointHistoryEntryRepository.save(entry);
        person.getPointHistory().add(entry);
        personRepository.save(person);
    }

    public List<PointHistoryEntry> getPersonPointHistory(String personId) {
        return pointHistoryEntryRepository.findByPersonId(personId);
    }
}