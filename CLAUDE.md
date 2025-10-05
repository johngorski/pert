# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

PERT is a Monte Carlo project estimation tool based on the US Navy's Program Evaluation and Review Technique. It models task durations as Beta distributions from three-point estimates (Low, Nominal, High), runs 10,000 simulations to produce probabilistic completion timelines, and visualizes results as Gantt charts and dependency graphs.

## Build System

This is a **Clojure/Leiningen** project. There is no npm/package.json.

```bash
lein repl          # Start REPL (primary development mode)
lein test          # Run all tests
lein test :only pert.core-test/test-name   # Run a single test
```

## Development Workflow

Development is REPL-driven using [Clerk](https://github.com/nextjournal/clerk) notebooks:

```clojure
;; In the REPL (src/pert/user.clj is the entry point):
(start-clerk!)     ; launches Clerk server at http://localhost:7777
```

Then open `http://localhost:7777/notebooks/demo.clj` to see the live demo notebook. Clerk hot-reloads on file changes.

Note: `^::clerk/no-cache` on a form forces re-evaluation (bypasses Clerk's cache). This is important when testing performance, since caching can hide slow code.

## Architecture

### Core Domain

- **`core.clj`** — PERT formulas: mean `(Lo + 4*Nom + Hi) / 6`, std dev `(Hi - Lo) / 6`, project-level combination
- **`random_variables.clj`** — Protocol-based probability distributions. The `Variable` protocol has a single `sample` method. Implementations: Beta, Gaussian, Uniform, constants. Composable via `sum-of`, `max-of`, `min-of`.
- **`task.clj`** — Task record with clojure.spec validation. A **backlog** is a validated sequence of tasks. Specs enforce: no circular deps, all dep IDs must exist, no self-deps.

### Scheduling Engine (`scheduling.clj`)

The simulation runs in discrete time steps:
1. Assign idle workers to ready tasks (deps complete)
2. Advance clock to the next task completion event
3. Mark tasks complete, repeat until backlog is empty

Key functions: `project` (single run), `simulator` (returns a reusable simulation fn), `simulations` (infinite lazy sequence of runs), `csv->ETE` (project end-time random variable).

### Data Input

- **`spreadsheet.clj`** — Generic row→task parsing with configurable column mapping; detects estimation type (3-point, Gaussian, uniform)
- **`csv.clj`** / **`excel.clj`** — Format-specific readers layered on top of `spreadsheet.clj`
- **`yaml.clj`** *(WIP)* — Hierarchical YAML task breakdown with auto-generated dependencies (`prev-deps`, `child-deps`, `parent-deps`)

### Visualization

- **`gantt.clj`** — SVG Gantt chart. Color encodes task status probability (Red=not started, Yellow=in-progress, Green=done). Cell heights represent probabilities from simulations.
- **`graph.clj`** — Task dependency graph utilities; topological sort via Kahn's algorithm; cycle detection
- **`mermaid.clj`** / **`graphviz.clj`** — Dependency diagram backends (Mermaid syntax; Graphviz DOT via Dorothy)
- **`report.clj`** — Percentile-based ETA reporting per task

### Key Design Patterns

- **Protocol dispatch** for random variable types (`Variable` protocol)
- **Multimethod dispatch** for estimation types (3-point vs Gaussian vs uniform)
- **Clojure spec** validates all task data at ingestion time before simulation
- **Priority maps** (`data.priority-map`) for efficient task scheduling
- **Pure functions** throughout; mutation only in random number sampling

## Data Flow

```
CSV / Excel / YAML
      ↓
spreadsheet.clj (rows → tasks with spec validation)
      ↓
Backlog (validated dependency graph)
      ↓
scheduling.clj × 10,000 runs (Monte Carlo)
      ↓
Gantt chart + status report (percentile ETAs)
```

## Test Data

Test CSV files are in `test/`: `example.csv` (bear assembly demo), `bear.csv`, `overcooked-burger.csv`, `comment-row.csv`.

## Performance Notes

The 13-task demo notebook takes ~6–7 seconds to evaluate. The main bottleneck is worker counting in the scheduler (known issue, see `DEVELOPMENT.md`). Use `(time form)` in the REPL to measure. The profiling guide at https://clojure-goes-fast.com/kb/profiling/ is referenced for future work.
