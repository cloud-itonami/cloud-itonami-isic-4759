# Business Model: Household Appliance, Furniture and Lighting Specialty Retail Operations Coordination

## Classification
- Repository: `cloud-itonami-isic-4759`
- ISIC Rev.5: `4759` -- retail sale of electrical household appliances,
  furniture, lighting equipment and similar household articles in
  specialized stores (household-appliance retailers, furniture stores,
  lighting-equipment specialty retailers; distinct from ISIC 4752's
  hardware/paint/glass specialty retail and ISIC 4719's general-
  merchandise retail)
- Social impact: local economy, consumer protection, transparency

## Customer
- independent household-appliance/furniture/lighting specialty stores
  needing an auditable operations-coordination platform
- multi-store operators needing consistent delivery/supply-order/
  warranty governance across sites
- programs that cannot accept closed, unauditable back-office platforms

## Offer
- sales/inventory/warranty-registration transaction logging
- delivery-and-installation scheduling coordination
- merchandise supply-order coordination with registered, verified vendors
- warranty/defect-dispute-concern flagging (refrigerator/washer/dryer
  compressor and motor failures, furniture-defect disputes, lighting-
  fixture failures) for human triage
- role-based access and immutable audit ledger

## Revenue
- self-host setup fee
- managed hosting subscription per store
- support retainer with SLA

## Trust Controls
- `:appliance-retail-governor` never lets a proposal for an
  unregistered/unverified store, or a supply order naming an
  unregistered/unverified vendor, commit or even escalate
- every proposal's `:effect` must be `:propose` -- a claim to directly
  actuate is a HARD, un-overridable block
- directly finalizing a warranty-claim decision (approving, denying or
  settling a claim, authorizing a refund or replacement) or a delivery/
  installation-safety clearance (certifying a gas/electrical hookup as
  safe) is permanently out of scope, not a rollout milestone -- the
  actor may only flag a concern for a human
- a `:flag-warranty-concern` proposal, and a high-cost `:coordinate-
  supply-order`, always require human sign-off
- sensitive customer, employee and supplier data stays outside Git
