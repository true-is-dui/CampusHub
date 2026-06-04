# CampusHub Frontend Coding Guidelines for Agents

This file defines frontend-specific rules for AI coding agents working on the CampusHub P4 coding stage.

These rules apply to all changes under the frontend project directory unless the user explicitly gives a more specific instruction.

## 1. Source of Truth

Follow the latest confirmed project documents and the user's latest decisions.

Primary references include:

- P1 requirement documents
- P2 architecture documents
- P3 detailed design documents
- `api_design.yaml`
- `database_design.md`
- `class_design.md`
- root-level `AGENTS.md`

Do not invent frontend pages, business flows, fields, status values, request parameters, or UI actions that are not supported by the latest documents or the user's explicit instructions.

If the frontend implementation appears to require an API field, status value, or workflow that is not defined in the documents, do not silently add it. Point out the issue and wait for confirmation before changing the contract.

## 2. API Contract Rules

The frontend must treat `api_design.yaml` as the source of truth for backend communication.

Follow these rules:

- Use the paths, HTTP methods, request bodies, query parameters, response fields, enum values, and error response format defined in `api_design.yaml`.
- Do not rename API fields for convenience unless the mapping is local, explicit, and isolated.
- Do not create frontend-only assumptions about backend status transitions.
- Do not hard-code undocumented backend behavior.
- Do not add temporary request fields, response fields, or status values without updating the API document after user confirmation.
- When an API design issue is found, report whether it is an API contract problem or a frontend implementation problem.

## 3. Business Flow Rules

The frontend must implement the confirmed CampusHub business flows.

Core flows include:

- User registration and login
- User profile display and editing
- Pickup request publishing
- Pickup request browsing and filtering
- Pickup request accepting
- Pickup request status display
- Pickup request completion
- Payment record display where required by the documents
- Review submission and display
- Notification display and unread handling where required by the documents
- File upload and file access where required by the documents

Do not add unsupported business features such as private chat, real-time WebSocket messaging, advanced admin workflows, or extra payment flows unless explicitly requested.

## 4. Form and Validation Rules

Frontend form validation must stay consistent with the API and requirement documents.

Follow these rules:

- Use the same required fields, length limits, enum values, and format restrictions defined in the API document.
- Do not loosen validation in a way that allows clearly invalid API requests.
- Do not make frontend validation stricter than the API unless it is explicitly confirmed.
- Display clear validation messages for required fields and invalid input.
- Keep campus options consistent with the confirmed campus list.
- Keep file upload requirements consistent with the API document and business documents.

Frontend validation is only a user-experience layer. Backend validation remains authoritative.

## 5. Authentication and Authorization Rules

Implement authentication behavior according to the confirmed API design.

Follow these rules:

- Store and send the authentication token using the project’s chosen frontend approach.
- Attach the token only through the shared request utility or a clearly centralized mechanism.
- Do not scatter manual token handling across pages.
- Handle unauthenticated responses consistently, usually by clearing invalid local auth state and redirecting to login.
- Do not show privileged actions to users who should not perform them according to the current business flow.
- Do not rely only on frontend hiding for authorization; backend authorization remains authoritative.

## 6. Request Utility and Error Handling Rules

Frontend API calls should go through a shared request layer.

The shared request layer should handle:

- Base URL configuration
- JSON request and response handling
- Authentication token attachment
- Common error response parsing
- Network error handling
- Unauthorized response handling
- Optional loading state conventions

Do not duplicate low-level request logic in each page or component.

For errors:

- Prefer showing user-readable messages from the backend when available.
- Keep technical details out of normal user-facing messages.
- Preserve enough error information for debugging during development.
- Do not ignore failed API calls silently.

## 7. Page and Component Organization

Keep frontend structure simple and maintainable.

Recommended organization:

- `api/` or `services/`: API request functions
- `components/`: reusable UI components
- `views/` or `pages/`: route-level pages
- `router/`: route definitions and guards
- `stores/` or `state/`: shared state if the project uses one
- `utils/`: small shared utilities
- `types/` or `models/`: frontend TypeScript types if TypeScript is used

Do not over-engineer the frontend with unnecessary abstractions.

Prefer clear, direct implementation that supports the required course project features.

## 8. State Management Rules

Use the simplest state management approach that fits the current project.

Follow these rules:

- Keep authentication state centralized.
- Keep current user information centralized if multiple pages need it.
- Avoid duplicating the same business data across unrelated components.
- Refresh data from the backend after important state-changing operations unless the local update is simple and safe.
- Do not assume a request succeeded before the backend confirms it.

If the project does not already use a dedicated state management library, do not introduce one without a clear need.

## 9. Routing Rules

Frontend routes should reflect the confirmed user-facing workflows.

Follow these rules:

- Protect pages that require login.
- Keep public pages accessible without authentication where appropriate.
- Do not create routes for unsupported features.
- Use clear route names and paths.
- Keep route guards simple and centralized.

If a route depends on a business object ID, validate the route parameter before calling the API when practical.

## 10. File Upload and Display Rules

File-related frontend behavior must follow the API document.

Follow these rules:

- Use the documented upload field names and content type.
- Do not invent extra file metadata fields.
- Validate file requirements according to the confirmed API and business documents.
- Display uploaded or related files only through documented access mechanisms.
- Handle missing, expired, or inaccessible files gracefully.

Do not bypass backend file access control by assuming static public paths unless the documents explicitly allow it.

## 11. Mock Data and Demo Data Rules

Mock data may be used only when it is clearly isolated from production API code.

Follow these rules:

- Do not mix mock data with real API calls in the same final flow.
- Do not hard-code fake business records in pages that are supposed to call the backend.
- If mock data is needed temporarily, mark it clearly and keep it easy to remove.
- Demo accounts or demo records should be created through documented initialization, seed data, or backend-supported flows.

The final implementation should prefer real backend integration over frontend-only simulation.

## 12. Testing and Verification Rules

When changing frontend behavior, verify the affected flow.

At minimum, check:

- Page loads correctly
- Required form fields validate correctly
- API request body matches `api_design.yaml`
- API response fields are consumed correctly
- Loading and error states are handled
- Auth-required pages behave correctly when logged out
- Core business operation completes through the UI

Do not treat a page as complete only because the static layout renders.

## 13. Styling Rules

Keep styling consistent with the existing frontend project.

Follow these rules:

- Reuse existing layout, spacing, and component conventions.
- Do not introduce a new UI framework unless explicitly requested.
- Avoid large visual rewrites unrelated to the requested task.
- Keep pages readable and suitable for course demonstration.
- Prefer simple and stable UI over complex animations.

## 14. Change Scope Rules

Make the smallest safe change that satisfies the current task.

Follow these rules:

- Do not refactor unrelated pages while implementing a feature.
- Do not rename files, routes, components, or API functions without a clear reason.
- Do not modify backend files from a frontend task unless the user explicitly asks or the frontend issue reveals a confirmed API/contract problem.
- Do not update project documents unless the user explicitly asks for documentation changes.

When a task touches both frontend and backend, clearly separate frontend changes from backend changes.

## 15. Final Response Rules for Agents

When reporting completed frontend work, summarize:

- What pages, components, or API functions were changed
- Which user flow was affected
- Whether the implementation follows `api_design.yaml`
- How the change was tested or checked
- Any remaining issue that requires user confirmation

Keep the summary concise and focused on implementation-relevant information.