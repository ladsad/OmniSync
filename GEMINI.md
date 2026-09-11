# GEMINI.md — Agent Instructions for OmniSync

This file is context for any AI agent (Gemini CLI or otherwise) working on the **OmniSync** project. Read this in full before making changes. If a requirement here conflicts with a one-off instruction in chat, ask before overriding it.

## Project Summary

OmniSync is a SaaS integration framework (Java + Python) that connects third-party apps — starting with Jira and HubSpot — via REST APIs, using Basic Auth and OAuth2, with paginated data extraction, resilient error/rate-limit handling, and 95%+ unit test coverage. Full spec: see `OmniSync-Project-Description.md` in this repo.

## Hard Constraints

### 1. Everything must be free
- Do not introduce any paid API, paid tier, paid SaaS dependency, or anything requiring a credit card — including "free trial" offerings that convert to paid.
- Prefer official free tiers of the target APIs (Jira/HubSpot developer sandboxes, free-tier API keys) over anything metered.
- For CI, testing, hosting, or tooling, only use free/open-source options (e.g., GitHub Actions free minutes, open-source libraries, self-hosted or local-only services).
- Before adding any new library, service, or external dependency, check it is free for this use case (no seat limits, no usage caps that would block normal development) and note this in the PR/commit description if it's not obviously free.
- If a genuinely free option doesn't exist for something the project needs, stop and flag it instead of substituting a paid one.

### 2. Documentation must be consistent
- Every connector, module, or public class/function gets a docstring/Javadoc following one fixed style (pick one and apply it everywhere — don't mix styles across Java and Python beyond the language's own convention).
- Maintain a `docs/` folder (or update `README.md` if the project stays small) that always reflects current behavior — update docs in the *same* commit as the code change that makes them stale, never as a separate deferred cleanup.
- Maintain `docs/PROJECT_LOG.md` as a chronological project log documenting all work done, architectural/engineering decisions, and problems/resolutions encountered; keep this file updated consistently alongside major changes and milestones.
- Each connector must document: auth method used, pagination style, rate-limit behavior, and known limitations.
- No TODOs left undocumented — if something is deliberately incomplete, say so in the docs, not just in code comments.

### 3. Git commits must be consistent and meaningful
- Small, atomic commits — one logical change per commit. Don't bundle unrelated changes.
- Commit message format:
  ```
  <type>: <short imperative summary, present tense, ~50 chars>

  <optional body: what changed and why, wrapped ~72 chars>
  ```
  Use `type` from: `feat`, `fix`, `refactor`, `test`, `docs`, `chore`, `perf`.
- **Never reference "phase", "step N", "part N", milestone numbers, or planning artifacts in commit messages.** Commit messages describe *what changed in the code*, not where it falls in a roadmap. 
  - Bad: `feat: phase 2 - implement OAuth2 handshake`
  - Good: `feat: implement OAuth2 token exchange and refresh flow`
- Write the message as if someone with zero context on the roadmap needs to understand the change from `git log` alone.
- No commits like `wip`, `fix stuff`, `updates`. If a change isn't ready to describe properly, it isn't ready to commit.

## Working Agreement for the Agent

- Follow the architecture in `OmniSync-Project-Description.md` (connector interface, auth strategy pattern, pagination abstraction, JSON parsing layer, resilience/retry layer) unless the user explicitly changes it.
- Write tests alongside code, not after — aim for the 95%+ coverage target incrementally, not as a final push.
- When adding a new connector, follow the existing connector's structure exactly so the pattern stays uniform.
- Ask before introducing a new external dependency, changing the auth strategy pattern, or changing the commit/docs conventions above.
