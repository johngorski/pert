Performance it terribad with the worker counting.
Something like 16 seconds to render a 13-task demo.
10 seconds for 10 tasks. So like a second per task, really not
great, and it probably scales worse than linear.

Turns out this has been the tool's performance already; the
issues was simply masked by clerk's cache. ^::clerk/no-cache
revealed an issue which was there before

SO! Let's make sure the test coverage is GREAT and then do some
performance optimization.


NEXT:
- Add in-progress task count per day
- Profile code for performance bottlenecks
- Include task p95 ETAs based on simulations
- Box and whisker charts for end times - may be easier to separate from current gantt table
- Continuous gantt visualization (rather than table cell per day)
- Rows without IDs in the spreadsheet can be comment rows, attached to nothing.
- Default un-estimated tasks to having estimates via Cauchy distributions and absolute values. Thanks kixi.stats!
  - Highlight un-estimated tasks in the gantt chart!

Focus: Simulator from backlog, not CSV. (scheduling/simulator backlog) taking workers
- We have (scheduling/project backlog durations workers)
- We'd like to get durations out of the estimates. Like, lots of them.
- Backing schema for gantt charts is a map from IDs to a sequence of day statistics

1. Move estimate multimethod to an estimate-specific namespace
1. In general, try to replace csv->* functions.
   - Remaining:
     - csv->ETE
     - csv->gantt-bar-html
1. Then probably kixi.stats and performance management.

Some ideas:
- delegate stats to kixi.stats for cljs and transducer-based performance improvements
- Overlay average number of concurrent tasks
- Spreadsheet validation.
  - Task IDs unique
  - All Dependency IDs task IDs
  - No dependency cycles
  - Low <= Estimate <= High
- Fill in Estimate for Low and High when not present
- Output namespaces
  - to table
  - to gantt chart
  - hiccup
- Project manipulation namespaces
- Monte Carlo simulation namespaces
- Random variable namespaces
- Stats namespaces
- What if gantt chart tasks were sorted by the last simulated end time

Some namespaces:
pert.core             ;; ??
pert.csv              ;; CSV parsing
pert.estimates        ;; Mapping of estimates to random variables
pert.gantt            ;; Gantt chart presentation
pert.graph            ;; Graph functions shared. DRY'd up graphviz + mermaid
pert.graphviz         ;; Graphviz presentation
pert.mermaid          ;; Mermaid diagram presentation
pert.random_variables ;; Random variable modeling
pert.scheduling       ;; Hindcasting/simulation of project schedules
pert.task             ;; Specs/protocols/etc. for tasks and dependencies

## Perf data

new duration reuse algorithm

Clerk evaluated 'notebooks/demo.clj' in 6829.652625ms.
Clerk evaluated 'notebooks/demo.clj' in 6229.582041ms.
Clerk evaluated 'notebooks/demo.clj' in 5821.971791ms.

old one, no duration reuse

Clerk evaluated 'notebooks/demo.clj' in 8862.953333ms.
Clerk evaluated 'notebooks/demo.clj' in 8418.459917ms.
Clerk evaluated 'notebooks/demo.clj' in 8551.114916ms.

Let's turn off caching everywhere

No cache on csv file and duration samples

old function

Clerk evaluated 'notebooks/demo.clj' in 10969.686541ms.
Clerk evaluated 'notebooks/demo.clj' in 9818.480917ms.
Clerk evaluated 'notebooks/demo.clj' in 11201.576542ms.

new function

Clerk evaluated 'notebooks/demo.clj' in 6039.146542ms.
Clerk evaluated 'notebooks/demo.clj' in 6661.859083ms.
Clerk evaluated 'notebooks/demo.clj' in 6608.522875ms.
Clerk evaluated 'notebooks/demo.clj' in 7006.828417ms.

Cool. A solid savings, then.
Can we do better? Probably.
Was this the best place to invest? Probably not.
Let's check a profiler next, that's the real test.

Well okay, one last check before the profiler: Use (time form) from the repl.








