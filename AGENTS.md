# AGENTS.md

## 1. Project identity

This repository is the backend repository for **SKHU Connect**.

- GitHub organization: `Skhu-Connect`
- Repository: `Skhu-Connect-BE`
- Base package: `org.skhuconnect`
- Stable branch: `main`
- Development branch: `dev`

Do not confuse this repository with any other hackathon, university project, personal project, or repository.

Requirements, code, architecture, naming conventions, and assumptions from other projects must never be introduced into SKHU Connect unless the user explicitly requests them.

---

## 2. Project overview

SKHU Connect is a student participation and petition platform for Sungkonghoe University.

The service is intended to provide a structured communication channel between students and the university.

Students can submit suggestions or petitions related to university life. Other students can express agreement with those petitions. When a petition reaches a defined agreement threshold, it can be reviewed by an administrator or the responsible university department.

The administrator or department should then provide one of the following:

- a resolution or implementation plan;
- the current review or processing status;
- a partial resolution;
- or a clear explanation of why the request cannot be implemented.

The main goal is not merely to create a discussion board. The service should make student opinions visible, reduce duplicated requests, expose the processing status, and preserve official responses.

The long-term objective is:

1. build a working service for university competitions and hackathons;
2. collect real student suggestions;
3. deliver accumulated suggestions to relevant university departments;
4. conduct meetings with university stakeholders, including a possible meeting with the university president;
5. propose SKHU Connect as an official university service.

The project should be treated as a real service with potential long-term operation, not as disposable competition-only code.

---

## 3. Confirmed product direction

### Student-facing service

The student-facing experience is expected to be mobile-centered.

Planned access channels include:

- a mobile application;
- a responsive web interface;
- shareable petition links that students can open in a browser.

The exact mobile framework and frontend implementation are outside the scope of this backend repository unless explicitly requested.

### Administrator service

Administrators are expected to use a web-based management interface because university administrative work is generally performed on desktop computers.

The backend should eventually support administrator functions such as:

- viewing submitted petitions;
- filtering and searching petitions;
- identifying petitions that reached their agreement threshold;
- assigning or recording responsible departments;
- changing review status;
- writing official responses;
- explaining rejected or infeasible requests;
- and managing duplicate or related petitions.

These functions are product direction, not automatically approved implementation requirements.

Do not implement them until the user requests the relevant task or the specifications are confirmed.

---

## 4. Confirmed core concepts

The following concepts are part of the current project direction:

- students submit petitions or suggestions;
- each petition belongs to a category;
- other students can agree with a petition;
- agreement counts are visible;
- petitions may have a review threshold;
- threshold rules may differ depending on the target group;
- petitions that reach the threshold can move to administrator review;
- administrators or university departments can provide official responses;
- unresolved or rejected requests should include an explanation;
- petition status and processing history should be visible;
- duplicate or similar petitions should eventually be detected, linked, or consolidated.

Possible target groups may include:

- all students;
- a specific department or major;
- dormitory residents;
- a specific grade or other defined student group.

The exact database schema, calculation logic, threshold values, and status enum values are not confirmed yet.

Do not invent or finalize those details without approval.

---

## 5. Initial petition categories

The current MVP category candidates are:

- Academic and Classes
- Scholarships and Student Welfare
- Facilities
- Dormitory
- Library

These categories may change after planning discussions or university feedback.

Do not hard-code them permanently without confirming whether they should be enums, database records, or administrator-managed data.

---

## 6. Future expansion

Possible future expansion includes:

- student community features;
- school announcements;
- surveys;
- discussions;
- notifications;
- personalized activity history;
- additional university communication functions.

These are future possibilities, not current implementation requirements.

Do not implement future features early unless explicitly requested.

---

## 7. Current technical environment

The currently confirmed backend environment is:

- Java 17
- Spring Boot 4.1
- Gradle Groovy
- Spring Web MVC
- Spring Data JPA
- Bean Validation
- MySQL
- MySQL Connector/J
- Lombok
- Spring Boot Actuator
- Spring Boot DevTools for local development
- Railway planned for deployment
- environment-variable-based database configuration

The local MySQL schema is currently:

```text
skhu_connect
```

The application has successfully started locally and connected to MySQL.

Spring Security was removed from the initial setup and is not currently part of the active dependency set.

The authentication and authorization strategy has not been finalized.

Do not assume JWT, OAuth2, university SSO, school email authentication, or another authentication method has already been selected.

---

## 8. Source of truth

Before making any change, use the repository itself as the primary source of truth.

Inspect:

1. the current Git branch;
2. `git status`;
3. `README.md`;
4. this `AGENTS.md`;
5. `build.gradle`;
6. `settings.gradle`;
7. `application.yml`;
8. relevant source files;
9. relevant test files;
10. existing Git diff.

Do not assume that a class, package, dependency, API, entity, table, configuration file, environment variable, or business rule exists without verifying it.

If this document conflicts with the actual repository, report the conflict before changing code.

Do not silently choose one source over the other.

---

## 9. Uncertainty and hallucination prevention

Never invent project facts.

When information is missing:

- explicitly state what is unknown;
- distinguish confirmed facts from assumptions;
- ask a question if the missing information affects architecture, security, database design, public APIs, or business rules;
- do not present a guess as an existing requirement.

Never claim that:

- a class exists without inspecting it;
- an API works without verifying it;
- a test passed without executing it;
- a build passed without executing it;
- the application started successfully without executing it;
- a database migration succeeded without verifying it;
- Railway deployment succeeded without checking the deployment result.

When uncertain, ask rather than assume.

---

## 10. Git and branch safety

`main` is the stable branch.

Development should occur on:

- `dev`;
- or a feature branch created from `dev`.

Do not modify project code while the current branch is `main`.

If the current branch is `main`, stop and inform the user before editing files.

Unless the user explicitly requests the exact Git operation, do not:

- commit;
- push;
- merge;
- rebase;
- reset;
- force-push;
- create a branch;
- delete a branch;
- rewrite Git history;
- discard user changes.

Never use force push.

Never run destructive Git commands without explicit user approval.

Do not stage unrelated user changes.

---

## 11. Work planning and change scope

Before modifying files, briefly report:

- your understanding of the request;
- the files you expect to inspect;
- the files you expect to change;
- whether the change affects APIs, database structure, dependencies, security, or existing behavior.

Do not begin a large architectural change without explaining the plan first.

If more than five files are expected to change, provide the implementation plan before editing.

After inspection, adjust the plan if the actual repository differs from the initial expectation.

Make the smallest reasonable change that fully satisfies the request.

Do not:

- perform unrelated refactoring;
- rename unrelated classes or packages;
- reformat unrelated files;
- implement speculative features;
- change existing behavior without explaining it;
- rewrite an entire file when a small edit is sufficient.

---

## 12. Dependency rules

Do not add, remove, or upgrade a dependency without a clear reason.

Before changing dependencies, report:

- the exact dependency;
- why it is needed;
- whether the existing stack can solve the requirement without it;
- expected compatibility with Java 17 and Spring Boot 4.1;
- potential runtime, test, deployment, and maintenance impact.

Do not introduce infrastructure or frameworks merely because they are popular.

Examples that require explicit approval include:

- authentication libraries;
- Flyway or Liquibase;
- QueryDSL;
- Redis;
- MongoDB;
- Kafka;
- WebFlux;
- Docker;
- AWS services;
- paid external services;
- additional databases.

Do not remove an existing dependency unless its impact has been inspected.

---

## 13. Avoid overengineering

Prefer a simple implementation that satisfies the confirmed requirement.

Do not create unnecessary:

- interfaces with only one implementation;
- abstract service layers;
- factories;
- providers;
- generic frameworks;
- utility classes;
- design-pattern wrappers;
- inheritance hierarchies;
- premature extension points.

An abstraction must solve an existing, concrete problem.

Do not introduce architecture for hypothetical future requirements.

Code quality matters, but complexity is not quality.

---

## 14. Basic Spring coding principles

Unless existing project conventions or confirmed requirements indicate otherwise:

- use constructor injection;
- prefer final dependencies;
- do not use field injection;
- do not use `@Autowired` on fields;
- keep controllers focused on HTTP request and response handling;
- do not place core business logic in controllers;
- do not access repositories directly from controllers;
- keep transaction boundaries in the service layer;
- do not return JPA entities directly from public APIs;
- separate request and response DTOs when an API is implemented;
- use validation annotations for request validation where appropriate;
- avoid public setters for every entity field;
- prefer explicit domain methods for meaningful state changes;
- avoid empty catch blocks;
- do not suppress exceptions without justification;
- do not expose stack traces, SQL errors, or internal exception messages to clients.

These are defaults, not permission to invent an unapproved architecture.

---

## 15. JPA and database safety

Before modifying database-related code, inspect the current entity and configuration state.

Default precautions:

- prefer lazy loading for entity relationships unless another strategy is justified;
- avoid unnecessary bidirectional relationships;
- do not use `CascadeType.ALL` without evaluating lifecycle effects;
- do not solve N+1 problems by changing all relationships to eager loading;
- do not add indexes without a query-based reason;
- do not change column nullability, uniqueness, length, or relationships without reporting the effect;
- do not manually delete or recreate tables;
- do not alter production or Railway data;
- do not assume `ddl-auto` behavior without inspecting `application.yml`.

Before a database-related change, explain:

- entities affected;
- columns affected;
- relationships affected;
- constraints affected;
- expected table changes;
- possible effect on existing data.

The current database design is not finalized.

Do not finalize the ERD through implementation without user approval.

---

## 16. API safety

The public API structure has not been finalized.

Do not invent permanent URLs, response envelopes, status enums, error-code formats, pagination formats, or authentication requirements unless requested or approved.

When implementing or changing an API, report:

- HTTP method;
- URL;
- request fields;
- validation rules;
- response fields;
- HTTP status codes;
- error cases;
- authentication and authorization requirements;
- backward compatibility impact.

Do not expose JPA entities directly.

Do not silently change an existing API contract.

---

## 17. Security and secrets

Never commit or expose:

- MySQL passwords;
- Railway credentials;
- access tokens;
- refresh tokens;
- JWT secrets;
- OAuth client secrets;
- API keys;
- personal information;
- `.env` contents.

Preserve the environment-variable-based configuration approach.

Do not replace environment variables with real local values.

Do not print secrets or personal authentication data in logs.

Do not weaken security controls merely to make a test pass.

Authentication and authorization are not yet finalized, so do not introduce a security design without approval.

---

## 18. Existing style and user work

Follow the existing repository style unless a change is necessary.

Do not rewrite files only to apply your preferred formatting.

Preserve user-written code and unrelated changes.

If the working tree already contains changes:

- inspect them;
- avoid overwriting them;
- clearly distinguish pre-existing changes from changes made during the current task.

Do not delete a file merely because it appears unused without verifying its purpose.

---

## 19. Testing and verification

Run verification appropriate to the scope of the change.

Windows commands may include:

```bash
gradlew.bat test
gradlew.bat build
gradlew.bat clean build
```

Use only the commands needed for the task.

When application startup or database access is relevant, verify that:

- MySQL is running;
- required environment variables are available;
- the configured schema exists;
- the application startup result is actually observed.

If a command fails:

1. preserve the relevant failure output;
2. identify whether the failure existed before the change;
3. determine whether the current change caused it;
4. fix it only if it is within the requested scope;
5. otherwise report it without hiding it.

Never report success for a command that was not executed.

If verification cannot be performed because of missing credentials, unavailable MySQL, unavailable network access, or another environment limitation, state that clearly.

---

## 20. ChatGPT and Codex collaboration

The user develops this project using both Codex and ChatGPT.

Architectural decisions, dependency changes, database changes, security changes, and public API changes must be reported clearly so they can be reviewed before integration.

Do not assume that a design created by Codex is automatically the final project design.

Provide enough detail for the user to copy the report into ChatGPT for an independent review.

Do not omit implementation details that would affect code review.

---

## 21. Required completion report

After every task that inspects, creates, modifies, or deletes project files, provide the following report.

### A. Request summary

State:

- what the user requested;
- how the request was interpreted;
- what was implemented;
- what was intentionally not implemented.

### B. Repository state before work

State:

- current branch;
- initial `git status`;
- relevant existing files inspected;
- any pre-existing uncommitted changes.

### C. Complete changed-file list

Separate the exact paths into:

- created files;
- modified files;
- deleted files.

If a category has no files, write `None`.

Do not omit generated, configuration, test, or documentation files.

### D. File-by-file change details

For every changed file, explain:

- what was added;
- what was changed;
- what was removed;
- important classes, methods, fields, annotations, or settings;
- the reason for the change;
- the behavior before and after the change.

Do not report only that a feature was implemented.

### E. Design decisions

Explain:

- why the selected implementation was used;
- alternatives considered, if relevant;
- trade-offs;
- impact on the existing architecture;
- assumptions requiring user review.

If no architectural decision was made, state that explicitly.

### F. Dependency changes

Report:

- added dependencies;
- removed dependencies;
- upgraded dependencies;
- exact Gradle coordinates;
- reason for each change;
- compatibility verification performed.

If there was no dependency change, write:

```text
No dependency changes.
```

### G. Database impact

Report:

- entities added or changed;
- fields or columns added, changed, or removed;
- relationships;
- constraints;
- indexes;
- expected schema changes;
- possible impact on existing data.

If there was no database impact, write:

```text
No database changes.
```

### H. API impact

For each created or changed API, report:

- method;
- path;
- request;
- response;
- status codes;
- validation;
- error cases;
- authentication and authorization.

If there was no API impact, write:

```text
No API changes.
```

### I. Verification performed

List every command actually executed.

For each command, report:

- success or failure;
- relevant result;
- failure cause if applicable.

Also state which relevant checks were not run and why.

Do not claim that a build, test, startup, database connection, or deployment passed unless it was actually verified.

### J. Remaining issues and risks

Report:

- incomplete work;
- temporary code;
- known limitations;
- assumptions;
- environment-dependent behavior;
- decisions still required from the user;
- possible follow-up work.

If none are known, write:

```text
No known remaining issues within the requested scope.
```

### K. Final Git state

Report:

- final branch;
- final `git status`;
- whether a commit was created;
- whether a push was performed;
- whether a merge was performed;
- recommended commit message.

Do not commit, push, or merge unless the user explicitly requested it.

---

## 22. Completion criteria

A task may be reported as complete only when:

- the confirmed request is implemented;
- unrelated files were not modified;
- existing user changes were preserved;
- no secret was exposed;
- relevant verification was actually performed;
- failures and limitations were reported honestly;
- every changed file was included in the completion report;
- architectural, database, dependency, security, and API impacts were disclosed.

If any required condition is not met, do not describe the task as fully complete.