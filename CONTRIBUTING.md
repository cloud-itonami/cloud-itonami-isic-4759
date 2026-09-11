# Contributing

`cloud-itonami-isic-4759` accepts contributions to the OSS blueprint,
capability bindings, policy tests, documentation and operator model.

## Development

```bash
kbb -M:test
kbb -M:lint
```

## Rules
- Do not commit real customer, employee, supplier or warranty/
  installation-safety-incident data.
- Keep sales-record logging, delivery-operation scheduling, supply-order
  coordination and warranty-concern flagging behind the
  ApplianceRetailGovernor.
- Treat appliance/furniture-store-operations workflows as high-risk: add
  tests for store/vendor verification, effect discipline, scope
  exclusion, escalation and audit logging.
- Never phrase a governor scope-exclusion term as a bare noun (e.g.
  "warranty", "claim", "installation") -- phrase it as the finalization/
  execution ACTION (e.g. "approved the warranty claim", "certified the
  gas hookup as safe"), and add/extend the
  `default-mock-advisor-proposals-never-self-trip-scope-exclusion`
  regression test for any new term. A bare-noun term will self-trip this
  actor's own legitimate `:flag-warranty-concern` happy path -- see
  `applianceops.governor/scope-excluded-terms`'s docstring.
- Never add an op that directly finalizes a warranty-claim decision or a
  delivery/installation-safety clearance to the closed proposal-op
  allowlist -- those actions are structurally excluded from this actor's
  vocabulary, not merely gated.
- Document any new business-model or operator assumption in `docs/`.

## Pull Requests
PRs should describe: what behavior changed, which policy invariant is
affected, how it was tested, whether operator or certification docs need
updates.
