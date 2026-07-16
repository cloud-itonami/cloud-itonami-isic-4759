# Governance

`cloud-itonami-isic-4759` is an OSS open-business blueprint for
household-appliance/furniture/lighting specialty-retail operations
coordination (ISIC Rev.5 4759 -- retail sale of electrical household
appliances, furniture, lighting equipment and similar household
articles in specialized stores).

## Maintainers
Maintainers may merge changes that preserve these invariants:
- a proposal for an unverified/unregistered store, or a supply order
  naming an unverified/unregistered vendor, can never commit.
- the ApplianceRetailGovernor remains independent of the advisor.
- hard policy violations (non-`:propose` effect, warranty-claim or
  installation-safety-clearance-finalization content, an op outside the
  closed allowlist) cannot be overridden by human approval.
- every sales-record log, delivery-operation schedule, supply-order
  coordination and warranty-concern flag is auditable.
- customer, employee and supplier data stays outside Git.

## Decision Records
Architecture decisions live in `docs/adr/`. Changes to the trust model,
storage contract, public business model, operator certification or
license should add or update an ADR.

## Operator Governance
Anyone may fork and operate independently. itonami.cloud certification is
a separate trust mark and should require security, audit and data-flow
review.

Certified operators can lose certification for:
- bypassing sale-record, delivery-scheduling, supply-order or warranty
  policy checks
- mishandling customer, employee or supplier data
- misrepresenting certification status
- failing to respond to security or warranty/installation-safety
  incidents
