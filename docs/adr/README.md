# Architecture Decision Records

This folder is FlyRes's design log. Each record (ADR) explains **what a feature does, how it is built across the BCE layers, and why** we chose that design over the alternatives. Together with the UML in [`../diagrams/`](../diagrams/), the ADRs are the documentation for the design report: the diagrams show the structure, and the ADRs give the reasons for it.

## When to write one

- **Every feature issue (`FLY-nn`) gets an ADR** in the same PR as its code. It documents the feature and every non-obvious decision made while building it.
- A cross-cutting decision that isn't tied to one feature (a new pattern, a convention change, a dependency) gets its own ADR.
- An accepted ADR is not rewritten later. If a decision changes, write a new ADR that **supersedes** the old one, and set the old one's status to `Superseded by ADR-NNNN`.

## How to write one

1. Copy [`0000-template.md`](0000-template.md) to `NNNN-short-title.md`, using the next free number.
2. Fill it in on the feature branch while you build. The *Decisions* section matters most: for each decision, list the options you considered and why you picked one.
3. Add a row to the index below.
4. Mention the ADR in the PR description.

## Index

| ADR | Title | Issue | Status |
| --- | --- | --- | --- |
| [0001](0001-record-architecture-decisions.md) | Record architecture decisions | — | Accepted |
| [0002](0002-authentication.md) | Authentication: login, customer registration, logout | FLY-04 | Accepted |
| [0003](0003-unexpected-error-handling.md) | Handling unexpected errors | FLY-28 | Accepted |
| [0004](0004-flight-search.md) | Flight search | FLY-05 | Accepted |
| [0006](0006-simulated-card-payment.md) | Simulated card payment | FLY-07 | Accepted |
| [0005](0005-flight-details-and-seat-map.md) | Flight details and seat map | FLY-06 | Accepted |
