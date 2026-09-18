# Design File — Event RSVP Manager

## Step 01 — Choose the setup

**Facts:**
- Tech stack: Java 17, Spring Boot 4, Spring MVC, Spring Data JPA, PostgreSQL, React 19, TypeScript, Vite
- AI partner for this design session: Claude Code (Sonnet 5), via VS Code
- Repo: `Xperience-Task-1-2026-04`, provided as an empty scaffold (no domain code yet)

**Open questions:**
- How design vs. implementation commits will be split, and whether this work is solo or reviewed, is not yet decided.

---

## Step 02 — Capture the raw feature brief

Raw brief, as given (unedited, verbatim from the task README):

- A user can create an event with a title, description, date/time, location, and optional max-capacity.
- The creator becomes the **host** of that event.
- The host can invite people by email.
- Each invitee receives a unique link and can respond: **Yes / No / Maybe**.
- The host sees a live attendance dashboard with counts and a list of attendees.
- If the event has a max-capacity and it is reached, new "Yes" RSVPs go to a **waitlist**.
- A waitlisted attendee automatically moves to confirmed if a confirmed attendee changes their RSVP to No.
- The host can cancel the event or close it to further responses at any time.
- An invitee can change their RSVP at any point **before** the event starts.
- After the event start time, all RSVPs are locked.

No editing or interpretation yet — that starts in Step 03.

---

## Step 03 — Write the problem statement

Hosts who want to run a small-to-mid-size event (a workshop, a dinner, a meetup) need to know **who is actually coming** without relying on ad-hoc tools (a spreadsheet, a group chat, a paper sign-up sheet) that don't track capacity, don't handle cancellations gracefully, and give invitees no simple way to respond or change their mind.

Today, when a host over-invites relative to a fixed-capacity venue, they either track a waitlist by hand or find out about overflow at the door. When an invitee's plans change, the host finds out late or not at all, and a freed spot silently goes to nobody instead of being offered to a waitlisted invitee.

This feature solves the coordination problem between one host and many invitees for a single event: a place to define the event, a low-friction way for invitees to respond without creating an account, an always-accurate capacity/waitlist state, and a live view the host can trust up to the moment the event starts, after which the guest list is final.

The problem is solved when a host can create an event, invite people, and — at any point before start time — get a correct, live answer to "who is coming, who is waitlisted, and how many seats are left," without manually reconciling responses themselves.

---

## Step 04 — Define goals and non-goals

**Goals:**
- Let a host create an event with title, description, start date/time, location, and an optional max-capacity.
- Let a host invite people by email, generating one unique, unauthenticated response link per invitee.
- Let an invitee respond Yes / No / Maybe via that link, and change their response any time before the event starts.
- Enforce max-capacity automatically: once confirmed attendance hits capacity, further Yes responses land on a waitlist.
- Automatically promote the longest-waiting waitlisted invitee to confirmed when a confirmed attendee frees a spot.
- Give the host a live dashboard: counts (confirmed / waitlisted / no / maybe / no response) and the attendee list.
- Let the host close an event (stop accepting new/changed responses) or cancel it, at any time.
- Lock all RSVPs automatically once the event's start time passes — no further changes by anyone.

**Non-goals (explicitly out of scope for this design):**
- User accounts, login, or identity verification for invitees. (Response links are the only identity mechanism — see Step 11.)
- Sending the actual invitation emails (SMTP/provider integration). The design assumes a link exists and is delivered to the invitee by some channel; delivery itself is an external dependency (see Step 14).
- Recurring events, multi-session events, or events with multiple ticket tiers/capacities.
- Payments, ticketing fees, or refunds.
- Real-time push updates to the host dashboard (e.g. WebSockets). Polling/refresh-on-load is sufficient for v1.
- Multi-host / co-host support for a single event.
- Public event discovery or a browsable event listing — events are only reachable by the host's session or an invitee's link.
- High-scale/viral event support (thousands of concurrent RSVPs on one event) — see Step 13.

---

## Step 05 — Capture context and constraints

**Context:**
- This is an educational exercise: a solo developer building a bounded feature into a pre-provided empty full-stack scaffold, evaluated primarily on the design artifact rather than production readiness.
- The scaffold ships with an unrelated leftover frontend (a bulk-messaging UI under `hero-frontend/src/components` and a `wasender` config block in `application.yml`) that is not part of this feature and will be replaced, not extended.
- No existing users, data, or traffic — this is greenfield.

**Constraints:**
- Backend stack is fixed: Java 17, Spring Boot 4, Spring MVC, Spring Data JPA, PostgreSQL. Frontend stack is fixed: React 19, TypeScript, Vite.
- Single local PostgreSQL instance (`localhost:5432`, db `hero`, schema `hero`), single backend instance on port 8280, single frontend dev server on port 5171 — no distributed deployment target is defined.
- `spring.jpa.hibernate.ddl-auto: update` is the current schema-management approach — acceptable for this prototype phase (see Step 16 for why it isn't acceptable beyond it).
- No authentication/authorization framework is currently wired into the scaffold — the host identity mechanism has to be designed, not assumed (see Step 06, Step 11).
- No outbound email capability is currently wired into the scaffold.
- Time-boxed educational task: design must be disciplined but is not expected to cover infrastructure concerns (CI/CD, multi-region, autoscaling) beyond acknowledging them as non-goals.

---

## Step 06 — Separate facts, assumptions, and open questions

**Facts** (stated directly in the brief or the scaffold):
- Event fields: title, description, date/time, location, optional max-capacity.
- Invitees are added by email and get one unique link each.
- Responses are exactly one of Yes / No / Maybe.
- Capacity overflow on Yes → waitlist.
- A confirmed→No transition can promote a waitlisted invitee.
- Host can cancel or close an event at any time.
- Invitee can change their response any time before the event start.
- After start time, all RSVPs are locked (read-only for everyone).
- Tech stack and ports are fixed by the scaffold (Step 05).

**Assumptions** (not stated; chosen here, and flagged so they can be challenged):
- **A1.** "Host" is a single authenticated identity per event; the design assumes *some* minimal session/identity mechanism exists to know who the host is, without building a full account system (non-goal). Treated as an external/stubbed concern — see Step 11.
- **A2.** An invitee is identified solely by possession of their unique link (no separate login) — this is a deliberate low-friction design choice, not just a gap-fill.
- **A3.** "Confirmed attendee changes their RSVP to No" (the brief's literal trigger for waitlist promotion) generalizes to **any transition out of Yes-confirmed** (No *or* Maybe both free the seat) — otherwise a confirmed attendee could switch to Maybe and silently sit on a seat that should be released. This is the only place the design intentionally goes beyond the brief's literal wording; flagged here rather than buried in Step 08.
- **A4.** Waitlist promotion order is FIFO by original Yes-response timestamp (first waitlisted, first promoted). The brief doesn't specify an order; FIFO is the simplest fair default.
- **A5.** "Close to further responses" (host action) means: no new responses and no changes to existing responses, but the dashboard and existing RSVP data remain visible/intact. "Cancel" means the event is dead — same lock, plus the event is flagged cancelled for display purposes. Neither deletes data.
- **A6.** Max-capacity, if set, counts **confirmed** Yes responses only; Maybe and waitlisted Yes do not count against it.
- **A7.** An invitee record (and its link) is created only when the host explicitly invites that email; there is no self-service "request an invite."

**Open questions** (need a stakeholder decision; explicitly not resolved by assumption):
- **OQ1.** What identity/auth mechanism does "host" actually use (Step 05/11)? Out of scope to build, but the interface it plugs into needs a placeholder decision before implementation.
- **OQ2.** How is the invite link actually delivered to the invitee (email provider, manual copy/paste)? Delivery is a non-goal, but the design assumes *a* channel exists.
- **OQ3.** Can a host re-open a Closed event, or is Close a one-way transition like Cancel? Not stated in the brief.
- **OQ4.** Can a host edit event details (title, time, capacity) after invitees have already responded, and if capacity is lowered, what happens to now-overflowing confirmed attendees?
- **OQ5.** Are waitlisted/promoted invitees notified of a promotion, and how (ties back to OQ2)?
- **OQ6.** Can the same email be invited twice to the same event (idempotency of invites)?

---

## Step 07 — Identify actors and workflows

**Actors:**
| Actor | Description | Identity |
|---|---|---|
| Host | Creates and owns an event | Assumed authenticated session (A1, OQ1) |
| Invitee | Was invited to an event by email | Possession of unique response-link token only (A2) |
| System (time) | Enforces the start-time lock | No identity; a derived condition evaluated on every request, not an actor with intent |

**Workflows:**

1. **Create event** (Host) → host submits title/description/start time/location/optional capacity → event created with host = creator, status = Scheduled.
2. **Invite invitees** (Host) → host submits a list of emails for an existing, not-yet-started event → one Invitee + unique token created per email, response = Pending.
3. **Respond to invitation** (Invitee, first time) → invitee opens their link → sees event details + current response (Pending) → submits Yes/No/Maybe → system applies capacity rule (Step 08 INV-1) → response recorded.
4. **Change response** (Invitee, repeat) → invitee reopens their link before event start → submits a new Yes/No/Maybe → system re-evaluates capacity/waitlist state, including possible promotion of another invitee (Step 08 INV-3).
5. **View live dashboard** (Host) → host views event → sees counts (confirmed, waitlisted, no, maybe, no-response) and the attendee list with each invitee's current state.
6. **Close event** (Host) → host closes the event → no further response changes accepted by anyone; dashboard remains viewable.
7. **Cancel event** (Host) → host cancels the event → same effect as Close, plus event is marked cancelled everywhere it's displayed.
8. **Auto-lock at start time** (System, passive) → on every read/write touching an event or its RSVPs, the system checks `now >= event.startTime`; if true, the event is treated as Locked regardless of stored status, and all write workflows (3, 4, 6, 7) are rejected.

---

## Step 08 — Define invariants

Named so they can be referenced from code, tests, and later sections.

- **INV-1 (Capacity invariant):** For an event with `maxCapacity` set, `count(RSVP where response = YES_CONFIRMED) <= maxCapacity`, at all times, under concurrent writes. Uncapped events (`maxCapacity = null`) have no such bound.
- **INV-2 (Lock-after-start invariant):** No RSVP for an event may be created, changed, or promoted once `now >= event.startTime`. This applies uniformly to invitee-initiated and system-initiated (promotion) writes.
- **INV-3 (Waitlist promotion invariant):** Whenever a seat is freed on a capacity-constrained event (a Yes-confirmed RSVP transitions to No or Maybe — A3) while the waitlist is non-empty and the event is not locked/closed/cancelled, exactly one waitlisted invitee (the longest-waiting, per A4) is promoted to confirmed in the same transaction that freed the seat.
- **INV-4 (One current response per invitee):** Each Invitee has exactly one current `response` value at any time (Pending/Yes/No/Maybe) and, when Yes, exactly one confirmation state (Confirmed or Waitlisted). There is no "multiple pending RSVPs" state.
- **INV-5 (Token uniqueness & stability):** Each Invitee's response token is unique per event, generated once at invite time, and never reused or reassigned — including after that invitee responds No or is later re-invited (A6/OQ6 dependent).
- **INV-6 (Host ownership invariant):** Every event has exactly one host, set at creation and immutable; only that host may invite, close, or cancel the event.
- **INV-7 (Terminal state invariant):** Once an event is Cancelled, or once it is Locked (by time or by Close), no further state transitions occur for the event or any of its RSVPs — these are terminal with respect to writes (reads remain allowed, except cancellation display rules).

---

## Step 09 — Generate first-pass architecture

```
┌─────────────────────┐        HTTPS/JSON        ┌───────────────────────────┐
│   React 19 + TS SPA  │ ───────────────────────▶ │   Spring Boot REST API    │
│   (Vite, port 5171)  │ ◀─────────────────────── │   (Spring MVC, port 8280) │
│                       │                          │                            │
│  Host views:          │                          │  Controllers:              │
│   - Create/Manage      │                          │   - EventController       │
│     Event               │                          │   - InvitationController │
│   - Dashboard            │                          │   - RsvpController        │
│  Invitee view:            │                          │                            │
│   - Respond (token link)   │                          │  Services (business rules):│
└─────────────────────┘        │                          │   - EventService           │
                                 │                          │   - InvitationService      │
                                 │                          │   - RsvpService (owns      │
                                 │                          │     capacity + waitlist    │
                                 │                          │     logic, INV-1/3)        │
                                 │                          │                            │
                                 │                          │  Repositories (Spring Data │
                                 │                          │  JPA): EventRepository,    │
                                 │                          │  InviteeRepository         │
                                 │                          └──────────┬─────────────────┘
                                 │                                     │ JDBC
                                 │                          ┌──────────▼─────────────────┐
                                 │                          │   PostgreSQL (schema hero)  │
                                 │                          │   events / invitees tables  │
                                 │                          └──────────────────────────────┘
```

**Key decisions:**
- Three controllers by responsibility (event management, invitation management, RSVP responses) rather than one monolithic controller — invitee-facing endpoints (RsvpController) are a distinct trust boundary from host-facing ones (Step 11).
- All capacity/waitlist/promotion logic lives in one place (`RsvpService`) so INV-1 and INV-3 are enforced in exactly one code path, not duplicated across "invitee responds" and "host views dashboard."
- Lock-after-start (INV-2) is a **computed** condition (`now >= startTime`), evaluated in the service layer on every write path — not a stored status flipped by a background job. No scheduler/cron dependency, no clock-sync-with-a-job-runner concern. Tradeoff discussed in Step 15.
- No message queue, cache, or search layer — traffic and data volume for this feature (one host, a bounded invitee list per event) don't justify it (Step 13).

---

## Step 10 — Define data ownership and state model

**Entities and ownership:**

| Entity | Owns | Written by |
|---|---|---|
| `Event` | title, description, startTime, location, maxCapacity, hostId, status (Scheduled/Closed/Cancelled) | Host only (create, close, cancel) |
| `Invitee` | email, eventId, token, response (Pending/Yes/No/Maybe), confirmationState (None/Confirmed/Waitlisted), respondedAt | Host writes `email`/creation; **only** the invitee (via token) writes `response`; the system writes `confirmationState` and `respondedAt` as a derived side-effect of `response` changes |

No separate "RSVP" table is required — an RSVP is 1:1 with an Invitee (one invitee, one current response), so `response` + `confirmationState` live directly on `Invitee`. History of past responses is not retained in v1 (no audit trail — flagged in Step 14).

**Event state machine:**

| From | Event | To | Trigger |
|---|---|---|---|
| Scheduled | host closes | Closed | Host action |
| Scheduled | host cancels | Cancelled | Host action |
| Closed | host cancels | Cancelled | Host action (closing then cancelling is allowed) |
| Scheduled / Closed | `now >= startTime` | Locked *(derived, not stored — see Step 09)* | System, passive |
| Cancelled | — | — | Terminal (INV-7) |
| Locked | — | — | Terminal (INV-7) |

Reopening Closed → Scheduled is **not modeled** (OQ3 is open; default behavior is one-way).

**Invitee response state machine** (per invitee, within a non-locked, non-closed, non-cancelled event):

```
                 ┌────────────┐
      ┌─────────▶│    NO      │◀─────────┐
      │          └────────────┘          │
      │                                    │
┌──────────┐   Yes, seat available   ┌───────────────┐
│ PENDING  │ ───────────────────────▶│ YES_CONFIRMED  │
└──────────┘                          └───────────────┘
      │                │  Yes, no seat            │  changes to
      │                ▼                          │  No or Maybe (A3)
      │          ┌───────────────┐                 │  → frees seat,
      │          │ YES_WAITLISTED │◀───────────────┘  triggers INV-3
      │          └───────────────┘        promotion
      │                │  changes to No/Maybe            promoted to
      │                ▼                                  YES_CONFIRMED
      │          (back to NO/MAYBE, no promotion needed —
      │           a waitlisted seat isn't a confirmed seat)
      └─────────▶┌────────────┐
                  │   MAYBE    │
                  └────────────┘
```

Any state may transition to any other reachable state while the event is open (an invitee can flip Yes→No→Yes repeatedly before start time); every Yes transition re-runs the capacity check (INV-1) at that moment, it does not remember a prior confirmation.

---

## Step 11 — Add trust boundaries and security notes

**Trust boundary 1 — Host vs. system:** The host is a semi-trusted authenticated actor (A1/OQ1). Every host-facing endpoint (`EventController`, `InvitationController`) must verify `event.hostId == currentUser.id` server-side (INV-6) before allowing create/invite/close/cancel — never trust a host ID passed from the client.

**Trust boundary 2 — Invitee vs. system:** The invitee is **unauthenticated**. Their only credential is the unique token in their link (A2). Consequences:
- The token must be a cryptographically random, unguessable value (e.g. UUIDv4 or equivalent — not a sequential invitee ID, not derived from email).
- Anyone with the link can respond *as* that invitee — this is an accepted, deliberate tradeoff for low-friction UX (A2), not an oversight. It should be stated plainly to hosts (e.g. "don't forward your invite link" is a UX/docs concern, not solvable in code).
- A leaked token cannot be individually revoked in v1 — the only mitigation is the host cancelling/closing the whole event. Per-invitee token rotation is a possible future improvement, not built now.
- The invitee-facing endpoint must return only that invitee's own record plus public event summary (title, time, location, counts) — never the list of other invitees' emails or tokens. This bounds what a leaked single token can expose.
- Tokens must never appear in server logs or be sent to third-party analytics; treat them as bearer secrets.

**Trust boundary 3 — Client input, generally:** All fields from both host and invitee requests are untrusted input at the API boundary: email format validated server-side, free-text fields (title, description, location) validated for length and sanitized/escaped on render to prevent stored XSS in the React dashboard, and response values restricted to the enum {Yes, No, Maybe} server-side regardless of what the client sends.

**Cross-cutting:**
- CORS must be scoped to the known frontend origin (`localhost:5171` in dev), not wildcarded.
- Rate-limit the invitee response endpoint per token to blunt brute-force token guessing and accidental double-submit storms — not built in v1, flagged as a pre-launch gap (Step 14/18).
- No sensitive data (PII beyond email) is collected, so no additional data-classification handling is required for this feature.

---

## Step 12 — Add concurrency and correctness notes

**The core race — two simultaneous Yes responses for the last open seat.**

Event has `maxCapacity = 10`, 9 confirmed, 0 waitlisted, 1 seat open. Invitee A and Invitee B, on different requests, both submit Yes at nearly the same instant.

*Naive (broken) implementation:* both requests read `confirmedCount = 9` in separate transactions, both see `9 < 10`, both write `YES_CONFIRMED`. Result: 11 confirmed, INV-1 violated.

*Design:* capacity check + write must be a single atomic, serialized operation per event, not read-then-write across two round trips. Two viable approaches, either acceptable, and the one chosen:

- **Chosen: pessimistic row lock.** The RSVP write path starts a transaction that does `SELECT ... FROM event WHERE id = :id FOR UPDATE` first, then counts confirmed invitees *within that same locked transaction*, decides Confirmed vs. Waitlisted, and writes the invitee row, then commits. The second concurrent request blocks on the row lock until the first commits, then sees the up-to-date count. This makes the two-writers-last-seat race impossible by construction rather than by retry.
- **Alternative considered:** an atomic conditional `UPDATE event SET confirmed_count = confirmed_count + 1 WHERE confirmed_count < max_capacity` with a stored counter, branching on affected-row-count. Rejected as the primary design because it requires maintaining a redundant `confirmed_count` counter column in sync with the actual invitee rows (extra invariant to protect), though it would scale better under heavy contention on one event (Step 13/15 tradeoff).

**The promotion race.** A confirmed invitee (A) changes Yes→No while, at the same moment, a waitlisted invitee (B) is *also* changing their own response (e.g. Waitlisted-Yes → Maybe, withdrawing themselves from the waitlist). Both writes must happen inside the same `SELECT ... FOR UPDATE`-guarded transaction pattern as above: A's transaction frees a seat and, still holding the lock, re-reads the current waitlist to pick who to promote — so it can never promote an invitee (B) whose own withdrawal hasn't committed yet, and vice versa. Promotion (INV-3) and the freeing write happen in the same transaction, never as a separate follow-up step, so there's no window where the seat is free but unpromoted and visible as such.

**Idempotency / double-submit.** An invitee double-clicking "Yes" (network retry, impatient click) must not be treated as two separate response events. The response endpoint is idempotent per token: submitting the same response value when it's already current is a no-op (no re-evaluation, no re-promotion trigger), detected before entering the locking transaction.

**Waitlist ordering under concurrency.** FIFO promotion order (A4) is anchored to `respondedAt` (the timestamp of the invitee's Yes-while-waitlisted transition), which is assigned inside the same locked transaction as the write — so ordering is consistent with commit order, not client-submitted timestamps (never trust a client clock for ordering).

**Lock-after-start races.** A response arriving at `startTime - 1ms` and completing at `startTime + 50ms` is evaluated against `now` at the moment the write transaction executes, not at request-received time — so a request that starts just before the deadline but commits just after is correctly rejected (INV-2), not just requests that start after.

---

## Step 13 — Add scalability and multi-tenancy notes

**Multi-tenancy:** Each event is independently owned and fully isolated by `eventId` / `hostId` scoping in every query — there is no cross-event data sharing, so "multi-tenancy" here just means many independent hosts/events coexisting in one database, not a shared-resource-per-tenant architecture. No tenant-level resource quotas are needed at this scale.

**Scale assumptions (explicit, not aspirational):** This is designed for the actual shape of the problem — one host, a guest list in the tens-to-low-thousands, one event at a time reaching its start deadline. It is *not* designed for:
- A single event with extremely high concurrent RSVP volume in a short window (e.g. a viral public event with thousands of simultaneous responses) — the `SELECT ... FOR UPDATE` row lock from Step 12 serializes all writes to one event's row, which is correct but becomes a throughput bottleneck under that load. Explicitly a non-goal (Step 04); the counter-based alternative from Step 12 would be revisited if this changed.
- Very large invitee lists (tens of thousands) per event — the dashboard's "list of attendees" view would need pagination, which isn't designed in v1.
- Horizontal scaling of the backend — single instance is assumed; if scaled out, the pessimistic-lock approach still works correctly across instances (it's a DB-level lock, not in-process), so this is a non-issue for correctness, only for the throughput ceiling above.

**What *is* built to scale within its intended range:** independent events don't contend with each other at all (locks are per-event-row), so many hosts running many concurrently-active events impose no cross-event bottleneck.

---

## Step 14 — Add risks and failure notes

| Risk | Impact | Notes |
|---|---|---|
| Invite-link delivery is out of scope (OQ2) but required for the feature to be usable | High — feature is inert without it | Needs an email provider decision before implementation; design assumes the link exists once generated, regardless of delivery channel |
| Host/auth mechanism undefined (OQ1) | High — INV-6 can't be enforced without knowing who "the host" is | Must be resolved before `EventController`/`InvitationController` can be implemented securely |
| Leaked invitee token | Medium — one invitee's RSVP can be impersonated | Accepted tradeoff (Step 11); no per-token revocation in v1 |
| No promotion notification (OQ5) | Medium — a promoted invitee may not know they're now confirmed until they revisit their link | UX gap, not a correctness gap; depends on OQ2 |
| No audit trail / response history | Low-Medium — disputes ("I said Yes!") can't be resolved from data | Only current state is stored (Step 10); acceptable for v1, flagged as a known limitation |
| `ddl-auto: update` schema management | Medium, grows over time | Fine for a prototype; risky once any real data exists (Step 16) |
| DB unavailable mid-RSVP-transaction | Low — standard failure mode | Transaction rolls back cleanly; client sees an error and can retry (idempotency from Step 12 makes retry safe) |
| Capacity lowered after RSVPs exist (OQ4) | Medium — could put an event over-capacity by simple edit | Not modeled; event-editing-after-invites is effectively a non-goal until OQ4 is resolved |
| Rate limiting absent on invitee response endpoint | Low-Medium | Flagged in Step 11 as a pre-launch gap, not built in v1 |

---

## Step 15 — Generate alternatives and tradeoffs

| Decision | Chosen | Alternative | Why chosen |
|---|---|---|---|
| Lock-after-start (INV-2) | Computed on read/write (`now >= startTime`) | Scheduled job flips a stored `LOCKED` status at start time | No scheduler dependency, no clock-drift-between-job-and-request risk, always correct even if the app was down at exact start time; cost is recomputing a cheap comparison on every request, which is negligible |
| Concurrency control for capacity (INV-1) | Pessimistic row lock (`SELECT ... FOR UPDATE`) per event | Atomic counter column (`UPDATE ... WHERE count < max`) | Simpler correctness reasoning (one lock, one source of truth: actual invitee rows) at the cost of serializing writes per event; acceptable given Step 13's scale assumptions. Would revisit if a single event needed very high write throughput |
| Invitee identity | Unauthenticated unique token link (A2) | Email + magic-link OTP each time, or full account signup | Matches the brief ("unique link... can respond") and keeps invitee friction near zero, which is the point of the feature; cost is the token-leak exposure accepted in Step 11 |
| RSVP storage shape | `response` + `confirmationState` fields directly on `Invitee` (Step 10) | Separate `Rsvp` entity/table with a 1:1 or 1:N relation to `Invitee` | 1:1 relationship and no history requirement (Step 04 non-goal) make a separate table pure overhead today; would split out if response history became a goal |
| Waitlist promotion trigger scope (A3) | Any transition out of Yes-confirmed (No or Maybe) | Only exact "changes to No" as literally stated in the brief | Prevents a silent capacity leak (switching to Maybe would otherwise hold a confirmed seat indefinitely); flagged explicitly as a brief-extension, not hidden |
| Waitlist promotion order (A4) | FIFO by response timestamp | Manual host selection, or random | Simplest fair default; brief doesn't specify, and manual selection adds host workflow complexity not requested |

---

## Step 16 — Add rollout / migration notes

- **Greenfield, no migration needed:** the scaffold has no existing `events`/`invitees` data, so there is no backfill or backward-compatibility concern for this feature's initial rollout.
- **Schema management must change before anything beyond local prototyping:** `ddl-auto: update` (Step 05) silently alters the schema on startup, which is unsafe once any real data exists or more than one developer touches the schema. Before rollout past this exercise, switch to a versioned migration tool (Flyway or Liquibase) with explicit, reviewed migration scripts, and set `ddl-auto: validate` (fail fast on drift) instead of `update`.
- **No feature flag needed:** this is the first and only feature being added to an empty scaffold — there's no existing traffic to protect from a partial rollout.
- **Leftover scaffold cleanup:** the pre-existing bulk-messaging components (`FileUpload`, `MessageComposer`, `RecipientTable`, `ResultsTable`, `bulkSendApi.ts`) and the `wasender` config block are unrelated to this feature and should be removed during implementation, not built on top of, to avoid confusing the codebase's intent.
- **Seed/test data:** implementation should include a small seed script or test fixtures (a sample event near/at capacity, with confirmed/waitlisted/no/maybe invitees) to exercise INV-1/INV-3 manually and in tests before relying on hand-testing through the UI.

---

## Step 17 — Assemble the first complete design draft

**System at a glance:** A host creates an Event (title, description, start time, location, optional capacity) and invites people by email; each invited person gets a unique, unauthenticated response link and answers Yes/No/Maybe, changeable any time until the event starts. Capacity is enforced by capping confirmed Yes responses (INV-1) and routing overflow to a FIFO waitlist (A4) that auto-promotes when a confirmed seat frees up (INV-3), computed atomically under a per-event row lock to survive concurrent responses (Step 12). Once the event's start time passes, or the host closes/cancels it, all writes are permanently rejected (INV-2, INV-7). The host gets a live read of counts and the attendee list at any time.

**Architecture recap (Step 09):** React SPA ↔ Spring Boot REST API (three controllers split by trust boundary: event management, invitations, RSVP responses) ↔ PostgreSQL, with all capacity/waitlist logic centralized in one `RsvpService`.

**Data model recap (Step 10):** Two entities — `Event` (host-owned) and `Invitee` (identity = token; response owned by the invitee, confirmation state derived by the system) — with explicit state machines for both.

**What's deliberately not solved yet (non-goals, Step 04, and open questions, Step 06):** authentication/identity for hosts (OQ1), invite delivery (OQ2), event editing after invites exist (OQ4), promotion notifications (OQ5), duplicate-invite handling (OQ6), and reopening a closed event (OQ3). These are named rather than silently assumed away, and implementation should not start on the areas they touch until resolved.

**This draft is the input to Step 18's weakness check, not the final word** — invariants, the architecture diagram, and the two state machines are the load-bearing parts; the rest (risk table, alternatives table) exists to make the *reasoning* behind those parts inspectable.

---

## Step 18 — Run the pre-review weakness check

Checked against the Definition of Success from the task README:

- [x] **Clear problem statement** — Step 03.
- [x] **Bounded scope (explicit non-goals)** — Step 04.
- [x] **Visible assumptions, separated from facts** — Step 06, with each assumption individually numbered and justified rather than blended into the facts.
- [x] **Explicit workflows for all key actors** — Step 07, 8 workflows across Host/Invitee/System.
- [x] **Named invariants** — Step 08, INV-1 through INV-7, referenced by name throughout later sections rather than restated.
- [x] **Real architecture boundaries** — Step 09, three controllers split specifically along the trust boundary from Step 11, not just by CRUD convenience.
- [x] **Explicit state ownership** — Step 10, field-by-field ownership table plus two full state machines.
- [x] **First-pass trust / concurrency / scale treatment** — Steps 11-13, including a concrete worked concurrency race (last-seat double-Yes) with a specific chosen mechanism, not just "handle races carefully."
- [x] **Visible risks and tradeoffs** — Steps 14-15, including where this design deliberately goes beyond the literal brief (A3) and why.
- [x] **Unresolved open questions listed** — Step 06 (OQ1-OQ6), cross-referenced from Steps 09, 11, 14, 17 rather than left orphaned in one section.

**Honest weaknesses found in this draft, not yet fixed (deliberately left open rather than papered over):**

1. **Two non-goals are load-bearing for the feature to actually work end-to-end**: auth (OQ1) and email delivery (OQ2) are both declared out of scope, but "host" and "invitee receives a link" are central to the brief. A reviewer should push back on whether these can really stay non-goals, or whether the design needs at least a *stubbed* interface (e.g. "assume a `CurrentHost` resolver and an `InviteLinkSender` port exist") so the rest of the design doesn't implicitly assume they're solved. **This design currently does not define those interfaces — it only names the gap.**
2. **A3 (promotion trigger generalized to Maybe, not just No) is a judgment call, not a confirmed requirement.** It's flagged honestly in Steps 06 and 15, but it is still a place where the design diverges from the literal brief on a guess about intent. A real stakeholder conversation could go the other way.
3. **No response-history/audit trail (Step 10, Step 14) means INV-3's "who gets promoted" decision is unverifiable after the fact** — if a host disputes why invitee X was promoted over invitee Y, there's no stored evidence beyond the current `respondedAt` timestamp. Acceptable for v1 scope, but worth flagging as the first thing to add if this moved toward real usage.
4. **The concurrency design (Step 12) is reasoned about, not yet tested.** A design-level argument for correctness under the pessimistic-lock approach is not the same as a passing concurrent-integration-test proving no double-booking under real parallel load — that test doesn't exist yet because there's no implementation yet.
5. **Rate limiting and token-guessing defense (Step 11) are named risks with no mitigation designed**, only deferred. If this feature were internet-facing rather than an educational local exercise, that gap would need to close before rollout.

None of the above block moving to implementation for this exercise's purposes, but all five should be revisited — in this order — before treating the feature as production-ready.
