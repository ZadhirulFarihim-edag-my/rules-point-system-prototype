# Gamification System

A point system prototype for tracking and rewarding team activities.

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Rules](#rules)
- [Technical Implementation](#technical-implementation)
- [How to Run](#how-to-run)
- [REST API Endpoints](#rest-api-endpoints)
- [Web Interface](#web-interface)
- [MySQL Migration](#mysql-migration)
- [Dynamic Rule Form](#dynamic-rule-form)
- [Automatic Rules Loading](#automatic-rules-loading)
- [Refactoring](#refactoring)
- [Development Notes](#development-notes)
- [Future Improvements](#future-improvements)

## Overview

This application is a gamification system that allows tracking and rewarding team activities through a point system. It
stores Person and Group entities in an H2 in-memory database and provides a web interface for managing them and
executing rules.

## Features

- Store Person and Group entities in H2 in-memory database
- Initialize the database with 20 persons and 5 groups
- Execute rules to award or penalize points
- Track point history for persons and groups
- Weekly cap reset for certain rules
- REST API for programmatic access
- Web interface with Thymeleaf templates

## Rules

The system includes the following rules:

1. **SAP Hours**: Manages rewards and penalties related to SAP hours logging.
    - Penalize -2 points to the offender's group
    - Award +1 point to non-offenders in the group (subject to a cap of 2 points per group per week)
2. **Forum Participant**: Award +1 point to the contributor's group
3. **Join Hackathon**: Award +5 points to the participant's group
4. **Win Team Game**: Award +10 points to the winner's group

## Technical Implementation

### Database Structure

- **Person**: Stores information about individuals, including their name, group, and point history
- **Group**: Stores information about teams, including their name, members, total points, and point history
- **PointHistoryEntry**: Tracks point changes for individuals
- **GroupPointHistoryEntry**: Tracks point changes for groups
- **ActivityPoints**: Tracks points awarded for specific activities to enforce caps

### Components

- **Entity Classes**: JPA entities for database storage
- **Repository Interfaces**: Spring Data JPA repositories for database operations
- **Service Layer**: Business logic for managing entities and executing rules
- **REST Controllers**: API endpoints for programmatic access
- **Web Controllers**: Controllers for the Thymeleaf web interface
- **Thymeleaf Templates**: Web interface for user interaction

## How to Run

1. Ensure you have Java 21 and Maven installed
2. Clone the repository
3. Run `mvn spring-boot:run` to start the application
4. Access the web interface at `http://localhost:8080`
5. Access the H2 console at `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:gamificationdb`, Username: `sa`,
   Password: `password`)

## REST API Endpoints

### Person Endpoints

- `GET /api/persons`: Get all persons
- `GET /api/persons/{id}`: Get a person by ID
- `GET /api/persons/name/{name}`: Get a person by name
- `GET /api/persons/search?name={nameFragment}`: Search persons by name
- `GET /api/persons/group/{groupId}`: Get all persons in a group
- `GET /api/persons/{id}/history`: Get a person's point history
- `POST /api/persons`: Create a new person
- `PUT /api/persons/{id}`: Update a person
- `DELETE /api/persons/{id}`: Delete a person

### Group Endpoints

- `GET /api/groups`: Get all groups
- `GET /api/groups/{id}`: Get a group by ID
- `GET /api/groups/name/{name}`: Get a group by name
- `GET /api/groups/search?name={nameFragment}`: Search groups by name
- `GET /api/groups/{id}/history`: Get a group's point history
- `GET /api/groups/{id}/activity/{ruleName}`: Get current points for an activity
- `POST /api/groups`: Create a new group
- `PUT /api/groups/{id}`: Update a group
- `DELETE /api/groups/{id}`: Delete a group
- `POST /api/groups/{groupId}/members/{personId}`: Add a member to a group
- `POST /api/groups/{groupId}/points`: Add points to a group
- `POST /api/groups/{groupId}/activity/{ruleName}`: Update activity points
- `DELETE /api/groups/{groupId}/activity/{ruleName}`: Reset activity cap

### Rule Endpoints

- `GET /api/rules`: Get all rules
- `GET /api/rules/{ruleName}`: Get a rule by name
- `POST /api/rules/execute/sap-offender`: Execute the SAP Hours rule for offenders
- `POST /api/rules/execute/forum-participant`: Execute the Forum Participant rule
- `POST /api/rules/execute/join-hackathon`: Execute the Join Hackathon rule
- `POST /api/rules/execute/win-team-game`: Execute the Win Team Game rule
- `POST /api/rules/execute`: Execute any rule

## Web Interface

The web interface provides the following pages:

- **Home**: Overview of the system with quick links
- **Persons**: List of all persons with details and point history
- **Groups**: List of all groups with details, members, and point history
- **Rules**: List of all rules with links to execute them
- **Rule Execution Forms**: Forms for executing specific rules

## MySQL Migration

This section provides information about the migration from H2 in-memory database to MySQL database for the Gamification
System.

### Changes Made

1. **Updated application.properties**
    - Replaced H2 database configuration with MySQL configuration
    - Added database connection parameters (port, name, username, password)
    - Configured SQL initialization settings

2. **Added MySQL Dependency**
    - Added MySQL Connector/J dependency to pom.xml

3. **Created SQL Initialization Script**
    - Created data.sql script to populate the database with initial data
    - Script includes the same data that was previously created programmatically

4. **Modified DatabaseInitializer**
    - Replaced Java-based data initialization with SQL script execution
    - Added fallback mechanism in case automatic script execution fails

### MySQL Database Setup

#### Prerequisites

- MySQL Server 8.0 or later installed
- MySQL client or administration tool (e.g., MySQL Workbench)

#### Setup Steps

1. **Create MySQL Database and User**

   ```sql
   CREATE DATABASE nest;
   CREATE USER 'nest'@'localhost' IDENTIFIED BY 'nest';
   GRANT ALL PRIVILEGES ON nest.* TO 'nest'@'localhost';
   FLUSH PRIVILEGES;
   ```

2. **Configure Application**

   The application is already configured to use the following MySQL settings:
    - Database URL: jdbc:mysql://localhost:3306/nest
    - Username: nest
    - Password: nest

   If you need to use different settings, update the following properties in `application.properties`:
   ```properties
   db.port=3306
   db.name=nest
   db.user=nest
   db.password=nest
   ```

3. **Run the Application**

   When the application starts, it will:
    - Connect to the MySQL database
    - Create the necessary tables (using Hibernate's schema generation)
    - Execute the data.sql script to populate the database with initial data

### Troubleshooting

#### Common Issues

1. **Connection Refused**
    - Ensure MySQL server is running
    - Verify the port number in application.properties

2. **Access Denied**
    - Verify the username and password in application.properties
    - Ensure the user has appropriate privileges on the database

3. **Tables Not Created**
    - Check the Hibernate DDL auto setting in application.properties
    - Verify that the entity classes have proper JPA annotations

4. **Data Not Populated**
    - Check the SQL initialization settings in application.properties
    - Verify that the data.sql script is in the correct location
    - Check the application logs for any SQL errors

#### Manual Database Initialization

If the automatic database initialization fails, you can manually execute the SQL script:

1. Connect to the MySQL database:
   ```
   mysql -u nest -p nest
   ```

2. Execute the SQL script:
   ```
   source /path/to/data.sql
   ```

### Additional Information

- The application uses Hibernate's `ddl-auto=update` mode, which will update the schema if entity classes change
- SQL initialization is configured to run after Hibernate creates the schema
- The DatabaseInitializer class serves as a fallback mechanism if automatic SQL script execution fails

## Dynamic Rule Form

### Overview

This section explains the implementation of a dynamic rule form in the Gamification System. The dynamic form replaces
multiple rule-specific HTML templates with a single template that adapts to different rule types based on their
configuration in `rules.json`.

### Changes Made

1. **Created a Dynamic Rule Form Template**
    - Created `rule-form.html` that dynamically generates form fields based on rule type
    - Implemented conditional rendering for different rule targets (single vs. multiple selection)
    - Added special handling for rule-specific fields (e.g., game type for Win Team Game)

2. **Updated WebController**
    - Replaced rule-specific methods with dynamic handlers
    - Added a method to render the dynamic form based on rule name
    - Implemented a generic form processing method that handles all rule types

3. **Updated Rules List Template**
    - Made the rules list dynamic to automatically include all rules from `rules.json`
    - Updated links to use the new URL format: `/rules/{ruleName}`

### Benefits

- **Maintainability**: Single template instead of multiple rule-specific templates
- **Scalability**: New rules can be added without creating new templates or controller methods
- **Consistency**: Uniform user experience across all rule types
- **Flexibility**: Rule configuration drives the UI, making it adaptable to changes

### How It Works

#### URL Structure

- **Rule List**: `/rules`
- **Rule Form**: `/rules/{ruleName}` (e.g., `/rules/sap-hours`)
- **Rule Execution**: `/rules/execute/{actionType}` (e.g., `/rules/execute/did_not_key_in_sap_hour`)

#### Rule Form Generation

The dynamic form adapts to the rule type based on its configuration in `rules.json`:

1. **Rule Name and Description**: Displayed at the top of the form
2. **Form Fields**: Generated based on the rule's target:
    - Single selection (dropdown) for most rules
    - Multiple selection (checkboxes) for "SAP Hours"
    - Additional fields for specific rules (e.g., game type for "Win Team Game")
3. **Submit Button**: Labeled based on the rule type (award or penalty)
4. **Instructions**: Dynamically generated based on the rule type

#### Adding New Rules

To add a new rule to the system:

1. Add the rule configuration to `rules.json`:
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

2. No template or controller changes are needed! The rule will automatically appear in the rules list and have a
   functioning form.

#### Special Considerations

1. **Multiple Participants**: If your rule needs to support multiple participants (like "SAP Hours"), set the rule name
   accordingly in the template condition:
   ```html
   <form th:if="${rule.ruleName == 'Your Rule Name'}" ... >
   ```

2. **Additional Fields**: If your rule needs additional fields, add them to the template with a condition:
   ```html
   <div th:if="${rule.ruleName == 'Your Rule Name'}">
     <!-- Additional fields here -->
   </div>
   ```

#### Example: Rule Execution Flow

1. User navigates to `/rules` to see the list of available rules
2. User clicks on a rule (e.g., "Forum Participant")
3. System loads the dynamic form for that rule at `/rules/forum-participant`
4. User selects a person and submits the form
5. System processes the form at `/rules/execute/forum_participation`
6. System redirects back to the rule form with a success message

## Automatic Rules Loading

### Overview

This section describes the changes made to ensure that rules are loaded automatically from `rules.json` when any rule
endpoint is executed.

### Changes Made

1. Added a new method `ensureRulesLoaded()` to the `RuleService` class:
   ```java
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
   ```

2. Added a constant for the path to the rules.json file:
   ```java
   // Path to the rules.json file
   private static final String RULES_JSON_PATH = "/rules.json";
   ```

3. Updated the `processEvent` method to call `ensureRulesLoaded()` before processing any event:
   ```java
   @Transactional
   public void processEvent(String actionType, Map<String, String> participants) {
       // Ensure rules are loaded before processing the event
       ensureRulesLoaded();
       
       // Rest of the method...
   }
   ```

4. Updated the `getRules` method to call `ensureRulesLoaded()` before returning the rules:
   ```java
   public Map<String, RuleConfig> getRules() {
       // Ensure rules are loaded before returning them
       ensureRulesLoaded();
       return Collections.unmodifiableMap(this.rules);
   }
   ```

### How It Works

1. When any rule endpoint is executed, it calls either `processEvent` or `getRules` in the `RuleService`.
2. These methods now call `ensureRulesLoaded()` before accessing the rules.
3. `ensureRulesLoaded()` checks if the rules map is empty, and if it is, loads the rules from `rules.json`.
4. This ensures that rules are loaded automatically when needed, rather than only at application startup.

### Benefits

1. **Automatic Loading**: Rules are loaded automatically when needed, without requiring manual initialization.
2. **Lazy Loading**: Rules are only loaded when they are actually needed, improving startup performance.
3. **Resilience**: If the rules file is updated, the changes will be picked up the next time a rule endpoint is
   executed.

### Testing

To test this functionality:

1. Start the application.
2. Make a request to any rule endpoint (e.g., `/api/rules` or `/api/rules/execute`).
3. Verify that the rules are loaded automatically and the endpoint works as expected.

## Refactoring

### Overview

This section describes the refactoring of the rule processing system in the Gamification application. The refactoring
focused on eliminating duplication between `RuleService` and `RuleEngine`, and ensuring that entity classes are used
consistently throughout the application.

### Changes Made

1. **Created EntityRuleEngine**
    - Implemented a new `EntityRuleEngine` class that works with entity classes and repositories
    - Moved rule processing logic from `RuleService` to `EntityRuleEngine`
    - Added proper transaction management with `@Transactional` annotations

2. **Simplified RuleService**
    - Refactored `RuleService` to delegate all functionality to `EntityRuleEngine`
    - Removed duplicated code between `RuleService` and `RuleEngine`
    - Maintained the same public API for backward compatibility

3. **Maintained Original RuleEngine**
    - Kept the original `RuleEngine` class for backward compatibility with `SimulationRunner`
    - This allows the simulation to continue working while the main application uses the new architecture

### Architecture

#### Before Refactoring

```
┌─────────────────┐     ┌─────────────────┐
│   RuleService   │     │    RuleEngine   │
├─────────────────┤     ├─────────────────┤
│ - processEvent  │     │ - processEvent  │
│ - loadRules     │     │ - loadRules     │
│ - getRules      │     │ - getRules      │
└─────────────────┘     └─────────────────┘
        │                       │
        ▼                       ▼
┌─────────────────┐     ┌─────────────────┐
│ Entity Classes  │     │  Model Classes  │
└─────────────────┘     └─────────────────┘
```

#### After Refactoring

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   RuleService   │     │ EntityRuleEngine│     │    RuleEngine   │
├─────────────────┤     ├─────────────────┤     ├─────────────────┤
│ - processEvent  │────▶│ - processEvent  │     │ - processEvent  │
│ - loadRules     │────▶│ - loadRules     │     │ - loadRules     │
│ - getRules      │────▶│ - getRules      │     │ - getRules      │
└─────────────────┘     └─────────────────┘     └─────────────────┘
                                │                       │
                                ▼                       ▼
                        ┌─────────────────┐     ┌─────────────────┐
                        │ Entity Classes  │     │  Model Classes  │
                        └─────────────────┘     └─────────────────┘
```

### Benefits

1. **Reduced Code Duplication**
    - Eliminated duplicate rule processing logic between `RuleService` and `RuleEngine`
    - Single source of truth for rule processing in `EntityRuleEngine`

2. **Improved Maintainability**
    - Changes to rule processing logic only need to be made in one place
    - Clearer separation of concerns between components

3. **Better Entity Usage**
    - Consistent use of entity classes in the main application flow
    - Proper transaction management for database operations

4. **Backward Compatibility**
    - Maintained the same public API for `RuleService`
    - Kept the original `RuleEngine` for simulation purposes

### Future Improvements

1. **Remove Model Classes**
    - Once `SimulationRunner` is updated to use entity classes, the model classes can be removed
    - This would further simplify the codebase and reduce duplication

2. **Update SimulationRunner**
    - Refactor `SimulationRunner` to use entity classes and `EntityRuleEngine`
    - This would allow for complete removal of the original `RuleEngine`

3. **Add Tests**
    - Add comprehensive tests for the new `EntityRuleEngine`
    - Ensure all rule processing scenarios are covered

## Development Notes

### Reference Documentation

For further reference, please consider the following sections:

* [Official Apache Maven documentation](https://maven.apache.org/guides/index.html)
* [Spring Boot Maven Plugin Reference Guide](https://docs.spring.io/spring-boot/4.0.0-SNAPSHOT/maven-plugin)
* [Create an OCI image](https://docs.spring.io/spring-boot/4.0.0-SNAPSHOT/maven-plugin/build-image.html)

### Maven Parent overrides

Due to Maven's design, elements are inherited from the parent POM to the project POM.
While most of the inheritance is fine, it also inherits unwanted elements like `<license>` and `<developers>` from the
parent.
To prevent this, the project POM contains empty overrides for these elements.
If you manually switch to a different parent and actually want the inheritance, you need to remove those overrides.

## Future Improvements

- Add authentication and authorization
- Add more rules and customization options
- Implement real-time updates with WebSockets
- Add charts and visualizations for point history
- Add export functionality for reports
- Implement a more sophisticated cap system
- Add unit and integration tests
- Remove model classes once SimulationRunner is updated to use entity classes
- Refactor SimulationRunner to use entity classes and EntityRuleEngine
- Add comprehensive tests for the new EntityRuleEngine