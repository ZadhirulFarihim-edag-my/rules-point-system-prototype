package com.edag.swd.my.gamification.repository;

import com.edag.swd.my.gamification.entity.Person;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PersonRepository extends JpaRepository<Person, String> {
    // Find all persons in a specific group
    @Query("SELECT p FROM Person p WHERE p.group.id = :groupId")
    List<Person> findByGroupId(String groupId);

    // Find a person by name
    Person findByName(String name);

    // Find persons by name containing a specific string (case insensitive)
    List<Person> findByNameContainingIgnoreCase(String nameFragment);
}