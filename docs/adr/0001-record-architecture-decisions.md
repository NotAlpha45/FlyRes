# ADR-0001: Record architecture decisions

- **Status:** Accepted
- **Date:** 2026-11-16
- **Issue:** —

## Context

FlyRes is graded on design quality and traceability as much as on features. The UML diagrams in `docs/diagrams/` show *what* the design is. Nothing in the repository yet records *why* it is that way, or which alternatives we turned down. Without that, the reasons get lost between team members and issues, and the design report has to be reconstructed from memory at the end of term.

## Decision

We keep Architecture Decision Records in `docs/adr/`, one Markdown file per record, numbered `NNNN-short-title.md`. The format is based on Michael Nygard's ADR template.

- Every feature issue (`FLY-nn`) ships an ADR in the same PR. That ADR is also the feature's documentation: what it does, how it maps onto the BCE layers, and every non-obvious decision with the options considered.
- Cross-cutting decisions get their own ADR.
- Accepted ADRs are not edited later. A changed decision gets a new ADR that supersedes the old one.
- [`README.md`](README.md) keeps the index and the instructions, and [`0000-template.md`](0000-template.md) is the template.

## Consequences

- Each PR carries a little more writing, but the design report can quote the ADRs directly.
- Reviewers can check a PR against its stated decisions instead of guessing what was intended.
- The decisions made in FLY-01 to FLY-03 (layering, SQLite, money as cents, and so on) are recorded in `CLAUDE.md` and the README, not as ADRs. ADRs start with FLY-04.
