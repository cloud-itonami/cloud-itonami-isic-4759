# cloud-itonami-isic-4759

Open Business Blueprint for **ISIC Rev.5 4759**: retail sale of electrical
household appliances, furniture, lighting equipment and similar household
articles in specialized stores -- household-appliance retailers, furniture
stores, lighting-equipment specialty retailers and similar household-goods
outlets, distinct from sibling ISIC 4752's hardware/paint/glass specialty
retail and ISIC 4719's general-merchandise retail.

This repository publishes a household-appliance/furniture/lighting-
specialty-retail operations-COORDINATION actor -- sales/inventory/
warranty-registration transaction logging, delivery-and-installation
scheduling coordination, merchandise supply-order coordination with
registered vendors, and warranty/defect-dispute-concern flagging -- as an
OSS business that any qualified operator can fork, deploy, run, improve
and sell, so an independent appliance/furniture store never surrenders
its operations data to a closed back-office SaaS.

Built on this workspace's
[`langgraph`](https://github.com/kotoba-lang/langgraph)
StateGraph runtime (portable `.cljc`, supervised superstep loop,
interrupts, in-mem/Datomic checkpoints) -- the same actor pattern as
every prior actor in this fleet -- here it is **ApplianceRetailAdvisor
⊣ ApplianceRetailGovernor**. This blueprint's own
`:itonami.blueprint/governor` keyword, `:appliance-retail-governor`, is a
distinct, independent build (no naming-collision precedent question --
distinct from ISIC 4752's own `:hardware-paint-retail-governor` and ISIC
4719's own `:merchandise-retail-governor`).

> **Why an actor layer at all?** An LLM is great at drafting a sales-
> record summary, a delivery-scheduling proposal, or a supply-order
> request -- but it has no license to actually finalize a warranty-claim
> decision (approving, denying or settling a claim against a delivered
> appliance/furniture item), no license to directly certify a delivery/
> installation-safety clearance (a gas or electrical hookup for a range,
> dryer or water heater), no way to independently confirm a store or a
> supply-order vendor is actually a registered/verified counterparty, and
> no notion of when a "flag this concern" op quietly turns into a claim
> to have already approved a warranty claim or certified an installation
> as safe. Letting it act directly invites an unverified store's data
> entering the ledger, an unverified vendor receiving a merchandise
> order, or -- worst of all -- a fabricated claim to have already settled
> a customer's warranty dispute or certified a gas/electrical hookup as
> code-compliant, exposing the shop, its customers and its delivery crew
> to real liability. This project seals the ApplianceRetailAdvisor into a
> single node and wraps it with an independent **ApplianceRetailGovernor**,
> a human **approval workflow**, and an immutable **audit ledger**.

## Scope: coordination only, never a warranty or installation-safety authority

This actor is **operations coordination only**. It never performs or
authorizes:

- setting or overriding a shelf/unit price
- directly finalizing a warranty-claim decision (approving, denying or
  settling a claim, authorizing a warranty refund or replacement, closing
  a claim as resolved)
- directly finalizing a delivery/installation-safety clearance
  (certifying a gas/electrical hookup as safe, clearing an installation
  hazard as resolved, signing off on an installation safety inspection,
  declaring an installation code-compliant)

The governor's `scope-exclusion-violations` check re-scans every
proposal for this failure mode independently of the advisor's own
framing, and treats it as a HARD, permanent block regardless of
confidence or how clean everything else is. Flagging a warranty/defect-
dispute concern for a human to triage is exactly this actor's job --
`:flag-warranty-concern` is never excluded by this check, only
FINALIZING/approving/denying that claim, or certifying an installation
as safe, is. **The closed proposal-op allowlist structurally never
includes any op that directly finalizes a warranty claim or an
installation-safety clearance -- there is no such op to gate, only one
to permanently exclude.**

### Actuation

**Every proposal this actor generates is `:effect :propose`, never a
direct actuation.** Two independent layers enforce this
(`applianceops.governor`'s `effect-not-propose-violations` HARD check and
`applianceops.phase`'s phase table, which never puts
`:flag-warranty-concern` in any phase's `:auto` set). A human store
operator/warranty coordinator is always the one who actually acts on a
flagged concern or confirms a high-cost supply order.

## The core contract

```
store/vendor registration + operations-coordination request
        |
        v
   ┌───────────────────────┐   proposal      ┌────────────────────────────┐
   │ ApplianceRetail-      │ ─────────────▶ │ ApplianceRetailGovernor      │  (independent system)
   │ Advisor (sealed)      │  + citations    │ store-unverified ·          │
   └───────────────────────┘                 │ vendor-unverified ·         │
          │                 commit ◀┼ effect-not-propose ·               │
          │                         │ scope-excluded (warranty-claim /    │
    record + ledger        escalate ┼ installation-safety-clearance       │
          │              (ALWAYS for│ finalization) · op-not-allowed      │
          │       :flag-warranty-   │                                      │
          │       concern/high-cost └────────────────────────────┘
          │       supply-order)
          ▼
      human approval
```

**The ApplianceRetailAdvisor never commits a proposal the
ApplianceRetailGovernor would reject, and a warranty/defect-dispute-
concern flag or a high-cost supply order never commits without a human
sign-off.** Hard violations (an unregistered/unverified store; an
unregistered/unverified supply-order vendor; a non-`:propose` effect;
content touching warranty-claim or installation-safety-clearance
finalization; an op outside the closed allowlist) force **hold** and
*cannot* be approved past.

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot
may perform physical domain work** (here: shelfing, picking, delivery
loading, floor merchandising, point-of-sale handling) under human/robot
floor operations gated by store policy. This actor itself does not
dispatch robot/hardware actions -- it is strictly the operations-
coordination layer (sales-record logging, delivery scheduling,
supply-order coordination, warranty-concern flagging) any physical-
dispatch layer could eventually feed proposals into, always gated the
same way by the independent ApplianceRetailGovernor.

## Features

- **Closed proposal-op allowlist**: `log-sales-record`,
  `schedule-delivery-operation`, `coordinate-supply-order`,
  `flag-warranty-concern` (all `:effect :propose`). No op in this
  allowlist finalizes a warranty claim or an installation-safety
  clearance.
- **Four HARD governor checks** (permanent, un-overridable):
  1. **Store unverified** -- the target store's business registration
     must exist AND be independently registered/verified in the store.
  2. **Vendor unverified** -- for `:coordinate-supply-order` only, the
     named vendor must exist AND be independently registered/verified --
     a supply-chain counterparty-verification gate.
  3. **Effect is :propose** -- any other `:effect` value is rejected.
  4. **Scope exclusion** -- directly finalizing a warranty-claim decision
     (approving, denying or settling a claim, authorizing a refund or
     replacement) or a delivery/installation-safety clearance
     (certifying a gas/electrical hookup as safe, signing off on an
     installation safety inspection) and an op outside the closed
     allowlist are both permanently blocked.
- **Two ESCALATE (SOFT) gates**, either forces human sign-off:
  - `:flag-warranty-concern` -- ALWAYS escalates, regardless of
    confidence or phase. A "flag a concern" op is never auto-commit
    eligible and never finalizes a warranty-claim decision itself -- it
    only surfaces the concern for a human.
  - `:coordinate-supply-order` above a cost threshold -- a large-value
    procurement proposal always needs a human sign-off.
  - (LLM confidence below the floor also escalates, as with every
    sibling actor.)
- **Staged rollout** (Phase 0→3):
  - Phase 0: read-only
  - Phase 1: sales-record logging only (approval-gated)
  - Phase 2: + delivery-operation scheduling, supply-order proposals
    (approval-gated)
  - Phase 3: auto-commits clean, high-confidence, low-cost proposals
    (warranty concerns and high-cost supply orders always escalate)
- **Append-only audit ledger** -- every decision is an immutable log
  entry.
- **langgraph-clj StateGraph** -- one request = one supervised run;
  human-in-the-loop via `interrupt-before`.

### Development

```bash
# Install dependencies (if inside the superproject, use :dev alias for local overrides)
clojure -M:dev -P

# Run tests
clojure -M:test

# Run linter
clojure -M:lint

# Run demo
clojure -M:run
```

### Test suite

- `test/applianceops/governor_test.clj` -- unit tests of governor hard
  checks, scope exclusion, and the self-trip regression test
- `test/applianceops/advisor_test.clj` -- advisor proposal shape and
  consistency
- `test/applianceops/phase_test.clj` -- rollout phase logic
- `test/applianceops/governor_contract_test.clj` -- full graph
  integration, audit trail
- `test/applianceops/store_contract_test.clj` -- Store protocol and
  MemStore implementation

### Modules

- `applianceops.store` -- SSoT (MemStore, String-keyed store/vendor
  directories, append-only ledger)
- `applianceops.advisor` -- contained intelligence node (mock +
  real-LLM seam)
- `applianceops.governor` -- independent compliance layer
- `applianceops.phase` -- staged rollout (0→3)
- `applianceops.operation` -- langgraph-clj StateGraph
- `applianceops.sim` -- demo driver

## Capability layer

This blueprint resolves its technology stack via
[`kotoba-lang/industry`](https://github.com/kotoba-lang/industry) (ISIC
`4759`).

## Business-process coverage (honest)

| Covered | Not covered (out of scope for this R0) |
|---|---|
| Sales/inventory/warranty-registration transaction logging (`:log-sales-record`) | Real POS/inventory-system integration |
| Delivery/installation scheduling coordination (`:schedule-delivery-operation`) | Direct crew dispatch/routing-system integration |
| Merchandise supply-order coordination with a registered, verified vendor, HARD-gated on vendor verification and a double-actuation-free single-proposal shape (`:coordinate-supply-order`) | Real supplier-ordering-system integration |
| Warranty/defect-dispute-concern flagging, ALWAYS human-gated (`:flag-warranty-concern`) | Directly finalizing any warranty-claim decision or installation-safety clearance -- permanently out of scope, not a gap |
| Immutable audit ledger for every log/schedule/order/flag decision | Daily reconciliation/cash-up -- a follow-up slice, not in this R0 |

Extending coverage is additive: add the next op (e.g. a return-
authorization or a cash-discrepancy-escalation check) as its own
governed op with its own HARD checks and tests, following the SAME "an
independent governor re-verifies against the actor's own records before
any real-world act" pattern this repo's flagship checks already
establish.

## Maturity

`:implemented` -- `ApplianceRetailAdvisor` + `ApplianceRetailGovernor`
run as real, tested code (see `Development` above), following the SAME
governed-actor architecture as every prior actor across this fleet, with
its own distinct, independently-named governor and its own warranty-
claim/installation-safety-clearance scope-exclusion check.

## License

Code and implementation templates are AGPL-3.0-or-later.
