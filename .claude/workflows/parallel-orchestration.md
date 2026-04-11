# Parallel Orchestration Workflow

Template for running multi-agent work where the main Claude Code session coordinates and sub-agents execute.

## Roles

- **Main agent** — orchestration only. MUST NOT read, edit, or run code directly. Its job is to decompose the task, dispatch sub-agents, aggregate their reports, and decide the next phase.
- **Sub-agents** — execute one scoped unit of work each. Run in parallel whenever their file clusters are independent.
- **Commit agent** — a dedicated, single, serial agent responsible for every git commit. No other agent commits.

## Parallelism rules

- Dispatch sub-agents in parallel whenever their working sets do not overlap. Partition by file cluster, not by task phase.
- If two sub-agents would touch the same file, serialize them — never let two agents race the same path.
- The main agent waits for all parallel sub-agents in a wave to report before starting the next wave.

## Commit rules

- Commits are serialized through the single commit agent. Sub-agents stage nothing and push nothing.
- Commit messages are one line. No `Co-Authored-By` trailers, no body text, no emoji.
- One logical change per commit. If a sub-agent produced two unrelated fixes, the commit agent splits them.

## Context hygiene

- When a sub-agent's remaining context budget drops to 5% or below, run `/compact` before continuing. Preserve: discovered solutions, final decisions, file paths that matter. Drop: debug transcripts, failed attempts, raw tool output.
- The main agent's own context should stay lean — it holds plans and summaries, not source code.

## Four-phase pattern

1. **Explore** — one or more read-only sub-agents map the relevant code, answer "what exists, where, and how is it wired?". Output: a written summary with absolute file paths.
2. **Design** — main agent (or a dedicated design sub-agent) turns the exploration into a plan: file clusters, sub-agent assignments, dependencies. Output: a task list with clear ownership boundaries.
3. **Implement** — parallel sub-agents edit their clusters. Each reports completion with a diff summary.
4. **Review → Simplify → Verify** — a review sub-agent reads the diffs, a simplify sub-agent removes dead code / redundancy, and a verify sub-agent runs the actual build/test commands. Only after verify is green does the commit agent run.

## Anti-patterns

- Main agent opens files "just to double-check" — forbidden; dispatch a sub-agent.
- Multiple agents committing in the same session — forbidden; the commit agent is the only writer of git history.
- Skipping the verify step because "the diff looks right" — forbidden; green build is the gate.
- Sub-agents spawning their own sub-agents — forbidden; only the main agent dispatches.
