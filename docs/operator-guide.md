# Operator Guide

## First Deployment
1. Register operator, stores and vendors; independently confirm each
   store's business registration and each vendor's registration before
   seeding `applianceops.store`.
2. Import existing sales/inventory/warranty-registration, delivery and
   supply-order history.
3. Run read-only sales-record-logging and delivery-operation dry-runs
   (Phase 0-1).
4. Configure the rollout phase and the `coordinate-supply-order`
   cost-escalation threshold for human sign-off paths.
5. Publish a dry-run warranty/defect-dispute-concern flag and audit
   export.

## Minimum Production Controls
- store-registration/verification check before ANY proposal for that
  store
- vendor-registration/verification check before ANY `:coordinate-
  supply-order` proposal
- governor gate on every proposal before commit
- human sign-off for `:flag-warranty-concern` (always) and high-cost
  `:coordinate-supply-order` proposals
- audit export for every commit, hold and approval
- backup manual back-office process

## Certification
Certified operators must prove store/vendor-verification discipline,
governor-bypass resistance, evidence-backed warranty/defect-dispute-
concern reporting and human review for every escalation-gated action.
Certification never covers directly finalizing a warranty-claim decision
or a delivery/installation-safety clearance -- those decisions always
stay with a qualified human/regulatory authority outside this actor.
