# CampusHub P4 Backend Guidelines for Agents

This file defines backend-specific rules for AI coding agents working on the CampusHub P4 coding stage.

These rules apply to backend code changes under this directory unless the user explicitly gives a more specific instruction.

The repository-level `AGENTS.md` still applies. This file only adds backend-specific implementation rules.

## 1. Backend Source of Truth

Follow the latest project documents and the user's latest confirmed decisions.

Primary backend references include:

- OpenAPI/API design document
- Database design document
- Class design document
- P3 detailed design documents
- P1 and P2 documents when business meaning or workflow is unclear

Do not invent backend fields, status values, database columns, API endpoints, permission rules, or business workflows that are not supported by the latest documents or the user's explicit instructions.

When documents appear inconsistent, point out the inconsistency instead of silently choosing one.

## 2. Backend Scope

Implement the CampusHub P4 MVP backend only.

Do not add backend support for extended features unless explicitly requested, including:

- Lost-and-found
- Second-hand trading
- Private chat
- Forum
- Partner matching
- Reporting or appeal workflow
- Real production payment
- WebSocket
- Message queue
- Redis optimization
- Elasticsearch
- Distributed services
- Complex admin backend beyond the confirmed scope

Do not create placeholder controllers, services, mappers, entities, tables, scheduled jobs, or configuration files for unrequested extended features.

Prefer a simple, reliable Spring Boot monolithic implementation suitable for a course project.

## 3. Layering Rules

Keep backend layering clear.

- Controller: request mapping, parameter receiving, authentication context extraction, response wrapping.
- Service: business rules, permission checks, state transitions, transaction boundaries.
- Mapper/Repository: database access only.
- Entity: database table mapping.
- DTO/VO: request and response objects for API communication.
- Config: framework and integration configuration only.
- Utility: reusable technical helpers only, not business workflows.

Do not put business logic, permission checks, state transitions, or cross-module workflow orchestration in Controllers, Mappers, Repositories, Entities, or Utilities.

Do not expose database entities directly as API response objects.

Use DTO/VO objects that match the OpenAPI document.

## 4. API Implementation Rules

The backend must implement the OpenAPI/API design document faithfully.

Do not rename, remove, reinterpret, or add the following without explicit approval:

- API paths
- HTTP methods
- Request fields
- Response fields
- Enum values
- Pagination fields
- Error response structure
- Time format
- File upload field names

Use the agreed unified response structure and error format consistently.

Do not return undocumented fields just because they exist in the database.

Do not rely on frontend behavior to enforce backend business rules.

Backend validation must still enforce required fields, field formats, enum values, permissions, and state constraints.

## 5. Service-Layer Business Rules

Business state changes must be centralized in the proper service layer.

Pickup request status changes must go through the pickup request service.

Payment status changes must go through the payment service.

Rating creation and rating-related business checks must go through the rating service.

Notification creation related to business events should be triggered by the relevant business service or a clearly defined notification service call.

File metadata creation and retrieval should go through the file service, while business modules decide whether a user may access business-related files.

Do not update business status fields directly in Controllers, Mappers, Repositories, or unrelated Services.

## 6. Pickup Request State Rules

Pickup request lifecycle logic must follow the latest confirmed business flow.

Do not create new pickup request status values unless explicitly requested.

Do not bypass documented state transitions.

Permission checks must be enforced before state transitions.

Typical pickup request operations include:

- Publishing a pickup request
- Paying for a pickup request
- Accepting a pickup request
- Completing a pickup request
- Cancelling a pickup request
- Querying user pickup request records

Each operation should verify the current user, current status, and allowed transition before updating data.

## 7. Payment Rules

Use Alipay sandbox integration only unless the user explicitly requests otherwise.

Do not add real production payment configuration.

Do not commit Alipay private keys, public keys, App IDs, merchant secrets, or local payment configuration.

Payment result handling may trigger pickup request business progression, but pickup request status should still be advanced through the pickup request service instead of being directly updated from unrelated modules.

Payment callback or payment result processing must be idempotent where practical, so repeated callbacks do not corrupt business state.

## 8. Transaction Rules

Use transactions for operations that update multiple related records or combine business state changes with notification, payment, rating, or file-binding records.

Typical transactional operations include:

- Publishing a pickup request
- Marking payment success or failure
- Accepting a pickup request
- Completing a pickup request
- Cancelling a pickup request
- Creating a rating
- Creating business notifications during state changes
- Binding uploaded files to business records when required

Avoid splitting one business action into multiple unrelated partial updates.

Do not use transactions in Controllers.

Keep transaction boundaries at the service layer.

## 9. File Module Rules

The file module is responsible for file storage metadata, upload, and retrieval.

Business modules are responsible for deciding whether the current user can access a business-related file.

Do not make the file module depend directly on specific business workflows unless the latest design explicitly requires it.

Do not store local absolute upload paths in committed configuration.

Do not commit uploaded files, temporary files, or environment-specific file storage directories.

Store only necessary file metadata in the database according to the database design document.

## 10. Authentication and Authorization Rules

Use JWT-based authentication according to the confirmed design.

Do not commit JWT secrets.

Do not hard-code user IDs, roles, tokens, or authentication bypasses.

Extract the current user through the agreed authentication mechanism instead of trusting client-provided user IDs.

Backend permission checks must be enforced in service logic for operations involving:

- User profile access and modification
- Real-name authentication data
- Pickup request ownership
- Pickup request accepting
- Pickup request completion
- Pickup request cancellation
- Payment records
- File access
- Rating creation
- Notification records

Do not rely only on frontend route guards or hidden buttons for authorization.

## 11. Database and Mapper Rules

Database operations must follow the database design document.

Do not add, remove, or rename tables, columns, indexes, foreign keys, or enum storage values unless explicitly requested.

Mappers or repositories should contain database access only.

Do not put business rules or permission checks in mapper SQL.

Avoid broad `SELECT *` in API-facing queries when a smaller field set is enough.

Keep SQL, entity fields, DTO/VO fields, and API response fields aligned.

## 12. Error Handling Rules

Use the agreed unified error response structure.

Do not leak stack traces, SQL errors, secrets, local paths, or internal implementation details to API responses.

Return clear business errors for:

- Invalid request fields
- Authentication failure
- Permission denial
- Resource not found
- Invalid state transition
- Duplicate or repeated operation
- Payment result inconsistency
- File access denial

Keep error codes and messages consistent with the API design document when defined.

## 13. Notifications

In-site notifications are part of the P4 MVP.

Create notifications for confirmed business state changes according to the latest design.

Do not introduce WebSocket, message queue, asynchronous delivery infrastructure, or push-notification systems unless explicitly requested.

Use simple database-backed in-site notification records suitable for polling.

Notification creation should not invent new notification types beyond the confirmed design.

## 14. Comments and Style

Follow the existing backend code style.

Use clear names consistent with the project documents.

Write comments only when they clarify non-obvious business logic, state transitions, security constraints, or integration behavior.

Do not add comments that merely repeat the code.

Do not refactor unrelated backend modules while implementing a feature.

## 15. Backend Validation Before Finishing

Before finishing a backend coding task, check that:

- The change stays within P4 MVP scope.
- The implementation follows the latest documents.
- API paths, request fields, response fields, enum values, and error format match the OpenAPI document.
- Business state transitions go through the proper service layer.
- Permission checks are enforced on the backend.
- Transaction boundaries are placed in the service layer when needed.
- Database operations match the database design document.
- No secrets, local files, uploaded files, or temporary files are introduced.
- No unrequested infrastructure or heavy dependencies are added.
- The modified code follows the existing backend style.
- The smallest relevant backend verification command has been run when practical.

If verification cannot be run, state this clearly and explain why.