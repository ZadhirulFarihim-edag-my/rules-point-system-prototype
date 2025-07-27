# Gamification System

A point system prototype for tracking and rewarding team activities.

## Overview

This application is a gamification system that allows tracking and rewarding team activities through a point system. It
stores Person and Group entities in a database and provides a web interface for managing them and executing rules.

## Tech Stack

- **Java 21**: Core programming language
- **Spring Boot**: Application framework
- **Spring Data JPA**: Data access layer
- **H2 Database**: In-memory database (default)
- **MySQL**: Optional persistent database
- **Thymeleaf**: Server-side Java template engine
- **Bootstrap 5**: Frontend framework
- **Maven**: Build and dependency management

## Features

- Store and manage Person and Group entities
- Execute rules to award or penalize points
- Track point history for persons and groups
- Weekly cap reset for certain rules
- REST API for programmatic access
- Web interface for user interaction

## Rules Engine Architecture

The rule engine is the core component of the gamification system:

```
┌─────────────────┐     ┌─────────────────┐
│   RuleService   │     │    RuleEngine   │
├─────────────────┤     ├─────────────────┤
│ - processEvent  │────▶│ - processEvent  │
│ - loadRules     │────▶│ - loadRules     │
│ - getRules      │────▶│ - getRules      │
└─────────────────┘     └─────────────────┘
                                │
                                ▼ 
                        ┌─────────────────┐
                        │ Entity Classes  │
                        └─────────────────┘
```

### Key Components

- **RuleConfig**: Defines rule properties including conditions and outcomes
- **OutcomeConfig**: Defines the effect of a rule (award or penalty)
- **ConditionConfig**: Defines when a rule should be triggered
- **RuleEngine**: Processes events and applies rules
- **RuleService**: Service layer that delegates to RuleEngine

### Rule Types

The system supports rules with:

- Single outcomes (award or penalty)
- Multiple outcomes (both award and penalty)
- Capped and uncapped point awards
- Weekly reset capabilities

## File Structure

```
src/
├── main/
│   ├── java/com/edag/swd/my/gamification/
│   │   ├── config/           # Configuration classes for rules
│   │   ├── controller/       # Web and REST controllers
│   │   ├── engine/           # Rule engine implementation
│   │   ├── entity/           # JPA entity classes
│   │   ├── models/           # Model classes
│   │   ├── repository/       # Spring Data repositories
│   │   ├── service/          # Service layer
│   │   └── GamificationApplication.java
│   │
│   └── resources/
│       ├── templates/        # Thymeleaf templates
│       │   ├── groups/       # Group-related templates
│       │   ├── layout/       # Layout templates
│       │   ├── persons/      # Person-related templates
│       │   └── rules/        # Rule-related templates
│       │
│       ├── application.properties  # Application configuration
│       ├── data.sql                # Initial data script
│       └── rules.json              # Rule definitions
│
└── test/                    # Test classes
```

## How to Run

1. Ensure you have Java 21 and Maven installed
2. Clone the repository
3. Run `mvn spring-boot:run` to start the application
4. Access the web interface at `http://localhost:8080`
5. Access the H2 console at `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:gamificationdb`, Username: `sa`,
   Password: `password`)

## How to Contribute

1. **Fork the Repository**: Create your own fork of the project
2. **Create a Branch**: Create a feature branch for your changes
3. **Make Changes**: Implement your changes following the project's coding style
4. **Add Tests**: Add tests for your changes if applicable
5. **Run Tests**: Ensure all tests pass
6. **Submit a Pull Request**: Create a pull request with a clear description of your changes

### Development Guidelines

- Follow Java coding conventions
- Use meaningful commit messages
- Document new features or changes
- Update the README if necessary
- Add appropriate tests for new functionality

### Adding New Rules

To add a new rule to the system, add the rule configuration to `rules.json`:

```json
{
   "ruleName": "New Rule Name",
   "description": "Description of the new rule.",
   "active": true,
   "groupBasedActivity": true,
   "conditions": [
      {
         "type": "action",
         "value": "new_rule_action"
      }
   ],
   "outcomes": [
      {
         "type": "award",
         "points": 5,
         "target": "participant",
         "reason": "New Rule Reason"
      }
   ]
}
```