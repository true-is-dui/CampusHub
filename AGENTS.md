# CampusHub P4 Coding Guidelines for Agents

This file defines repository-wide rules for AI coding agents working on the CampusHub P4 coding stage.

These rules apply to all frontend, backend, and shared project changes unless the user explicitly gives a more specific instruction.

## 1. Source of Truth

Follow the latest project documents and the user's latest confirmed decisions.

Primary references include:

- P1 requirement documents
- P2 architecture documents
- P3 detailed design documents
- OpenAPI/API design document
- Database design document
- Class design document

Do not invent business rules, fields, status values, modules, pages, API endpoints, or workflows that are not supported by the latest documents or the user's explicit instructions.

When documents appear inconsistent, do not silently choose one. Point out the inconsistency and make the smallest safe change only if the user has already confirmed the intended direction.

## 2. P4 MVP Scope

P4 coding stage focuses on the CampusHub MVP only.

Implement only the following core features unless explicitly requested otherwise:

- User registration and login
- JWT-based authentication
- User profile and real-name authentication
- Pickup request publishing
- Pickup request payment
- Pickup request accepting
- Pickup request completion
- Pickup request cancellation
- User pickup request records
- Alipay sandbox integration
- File upload and file access
- Rating and review
- In-site notifications

Do not implement the following extended features unless explicitly requested:

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
- Docker or deployment restructuring
- Complex admin backend beyond the confirmed scope

Do not add database tables, API endpoints, DTOs, frontend pages, placeholder services, or navigation entries for extended features unless explicitly requested.

Prefer a simple, reliable monolithic implementation suitable for a course project.

## 3. API Contract Rules

The OpenAPI/API design document is the contract between frontend and backend.

Do not rename, remove, or reinterpret the following without explicit approval:

- API paths
- HTTP methods
- Request fields
- Response fields
- Enum values
- Pagination fields
- Error response structure
- Time format
- File upload field names

Backend implementation should match the API document.

Frontend implementation should consume the API as documented.

Use the agreed unified response structure and error format consistently.

Do not make frontend or backend code depend on undocumented response fields, undocumented enum values, or guessed backend behavior.

If implementation reveals that the API document is incomplete or inconsistent, report the contract issue instead of silently changing code.

When an API-related problem appears, clearly distinguish among:

- API document problem
- Backend implementation problem
- Frontend implementation problem
- Test data problem

If the user confirms an API contract change, update the affected frontend and backend implementation consistently.

## 4. Business Flow and State Rules

Business state changes must follow the latest confirmed business flow documents.

Do not create new status values, cancellation reasons, payment states, rating states, authentication states, or notification types unless they are documented or explicitly requested.

Pickup request, payment, file, rating, and notification behavior must stay consistent across:

- API contract
- Backend implementation
- Frontend display logic
- Database schema
- Project documents, when documentation changes are explicitly requested

Do not invent or modify rules about:

- Who can create, accept, cancel, complete, pay for, or review a pickup request
- Which status transitions are allowed
- Which user role can see or operate on a business object
- Which fields are required for publishing or accepting a request
- Which files are required or optional
- How cancellation, expiration, payment, completion, review, and notification should behave

When implementing or modifying a business flow, keep the change aligned across frontend and backend if both sides are affected.

## 5. Frontend and Backend Boundary

Keep frontend and backend responsibilities clear.

Frontend should:

- Follow the documented API contract.
- Use documented request and response fields.
- Display documented status values and error messages appropriately.
- Perform basic form validation according to documented field constraints.
- Handle JWT login state consistently.
- Avoid inventing business rules that should belong to the backend.

Backend should:

- Enforce business rules, permission checks, and state transitions.
- Validate requests according to documented constraints.
- Return the unified response and error format.
- Avoid exposing database entities directly as API responses.
- Avoid changing API behavior without explicit approval.

When implementing a feature that involves both sides:

- Backend DTOs, controllers, services, validation rules, and returned JSON must align with the API document.
- Frontend request functions, form fields, route parameters, response parsing, and page state must align with the API document.
- Error messages may be user-friendly, but the error structure must remain consistent with the API contract.
- Pagination, filtering, sorting, file upload, authentication, and authorization behavior must be handled consistently on both sides.
- Do not temporarily change one side to fit the other side without checking the API contract.

## 6. Security and Configuration Rules

Never commit real secrets or local private configuration.

Do not commit:

- JWT secrets
- Alipay private keys
- Alipay public keys
- App IDs or merchant secrets
- Local database passwords
- Local absolute upload paths
- Personal tokens
- IDE-specific sensitive files
- Uploaded local files
- Temporary files
- Environment-specific generated files

Use environment variables, local ignored configuration files, or example configuration files.

If example configuration is needed, provide placeholders only.

## 7. Technology Constraint

Do not introduce new infrastructure or heavy dependencies unless explicitly requested.

Avoid adding:

- WebSocket
- Redis
- Message queue
- Elasticsearch
- Distributed services
- Complex deployment scripts
- Unnecessary code generators
- Unconfirmed framework migrations

Use the existing project architecture and keep the implementation simple.

## 8. Change Scope Discipline

Make the smallest coherent change needed for the current task.

Do not refactor unrelated modules while implementing a feature.

Do not modify API, database schema, documented business behavior, or frontend route structure unless the user explicitly asks for that change.

When a task requires changing multiple layers, keep the change aligned across:

- API contract
- DTO/VO or frontend type definitions
- Backend service logic
- Database schema
- Frontend caller and page logic
- Error handling and status display

Do not silently leave one layer inconsistent with another.

Do not add unsupported features, placeholder modules, or navigation entries merely for future expansion.

## 9. Testing Rules

Testing is required for core business logic and integration-sensitive code.

Backend changes should include or preserve tests for:

- Parameter validation
- Authentication and authorization checks
- Pickup request status transitions
- Duplicate or invalid operations
- Payment-related state handling where applicable
- Review submission constraints where applicable
- Notification creation or unread handling where applicable
- File upload and file access rules where applicable

Frontend changes should be checked for:

- Page rendering
- Required form validation
- Correct request body and query parameters
- Correct response field usage
- Loading, empty, and error states
- Auth-required page behavior
- Core user operation flow

Do not write meaningless tests that only check that a method, component, or page exists.

Tests should verify real behavior, real validation, or a real contract.

If a test cannot be added or run within the current task, state this clearly in the final response.

## 10. Core Flow Verification

Before treating the P4 implementation as ready, the following core flow should be runnable according to the latest confirmed documents:

1. Register or log in as a user.
2. Publish a pickup request with required information.
3. Browse or query pickup requests.
4. Accept an available pickup request.
5. Move the pickup request through the confirmed status flow.
6. Complete the pickup request.
7. Submit and display a review where required.
8. Display related notification or status update information where required.

Do not add unsupported steps merely to make the demo look richer.

Do not bypass documented business rules only to make the demo flow pass.

## 11. Abnormal Flow Verification

The following abnormal cases should be considered during implementation and testing:

- Unauthenticated access
- Unauthorized operation on another user's resource
- Missing required fields
- Invalid enum or status value
- Invalid campus or contact field
- Invalid file upload request
- Accepting an unavailable pickup request
- Repeating an operation that should only happen once
- Performing an operation in the wrong status
- Accessing a nonexistent or deleted resource

The implementation should return or display clear errors instead of failing silently.

## 12. Demo Data Rules

Demo data must be realistic, repeatable, and easy to reset.

Follow these rules:

- Prefer seed data, initialization scripts, or documented setup steps over hard-coded frontend data.
- Do not mix fake frontend-only records into real API flows.
- Demo accounts should use clearly non-sensitive test information.
- Demo pickup requests should cover the main statuses needed for presentation.
- Demo data should not bypass normal business rules unless explicitly marked as test setup.
- If a demo requires special data, document how to create or reset it.

## 13. Bug Fix Recording Rules

When fixing a bug, the final response should record:

- The visible symptom
- The root cause if identified
- The files or modules changed
- The verification performed
- Any remaining risk or follow-up needed

Do not describe a bug as fixed unless it has been checked through a relevant test, manual verification, or clear reasoning based on the changed code.

## 14. Comments and Documentation

Write comments only when they clarify non-obvious business logic or important constraints.

Do not add excessive comments that merely repeat the code.

When changing behavior that affects API usage, database schema, or business flow, update the corresponding project document only if the user explicitly asks for documentation changes.

Do not create new documentation files unless explicitly requested.

## 15. Validation Before Finishing

Before finishing a coding task, check that:

- The change stays within P4 MVP scope.
- The implementation follows the latest documents.
- API fields and enum values match the OpenAPI document.
- Frontend and backend remain consistent where both are affected.
- Business flow and status handling match the confirmed design.
- Authentication and authorization behavior remain valid.
- Required validation and error handling are implemented.
- No secrets, local files, uploaded files, or temporary files are introduced.
- The change does not add unrequested infrastructure.
- The modified code is consistent with the existing project style.
- The smallest relevant verification command has been run when practical.

If verification cannot be run, state this clearly and explain why.

## 16. Final Response Rules for Agents

When reporting completed coding work, summarize:

- What was changed
- Which user flow or module was affected
- Whether the implementation follows the API contract
- What verification was performed
- Any remaining issue that requires user confirmation

Keep the summary concise and focused on implementation-relevant information.