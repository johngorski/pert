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
- Overlay average number of concurrent tasks
- delegate stats to kixi.stats for cljs, transducer-based performance improvements, and Cauchy distributions
- Then probably kixi.stats and performance management.
- Profile code for performance bottlenecks: https://clojure-goes-fast.com/kb/profiling/
- Include task p95 ETAs based on simulations
- Move estimate multimethod to an estimate-specific namespace
- Box and whisker charts for end times - may be easier to separate from current gantt table
  - kixi.stats for stats
  - Vega-lite support for box-and-whiskers/other visualizations/visualization grammar
- Show cumulative distribution visualizations for tasks
- Continuous gantt visualization (rather than table cell per day)
- Rows without IDs in the spreadsheet can be comment rows, attached to nothing.
- Default un-estimated tasks to having estimates via Cauchy distributions and absolute values. Thanks kixi.stats!
  - Highlight un-estimated tasks in the gantt chart
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
- Other project necessities
  - Sequence diagram from spreadsheet (from message to)
  - Dataflow diagram from spreadsheet (from message to)
  - Link task breakdown to components in data flow diagram needing change/implementation
  - STATUS REPORTING
    - Multiple sheets of project estimates over time, for review and tracking
    - Status fields in backlog spreadsheet to report when tasks started/ended
  - Ingesting task breakdown data externally
    - CSV can implement the protocol
    - Calls to issue management web services would be another implementation
    - Caching can be quite helpful in both cases
- Editor via react + re-frame
- Make cell tooltips sensitive to the number of project simulations:
  - Percentages don't make any sense for < 100 simulations, show fractions
- Make task dependency graphs sensitive to depth and breadth
  - Find/implement a git commit log-style visualization to illustrate task dependencies compactly.

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
https://clojure-goes-fast.com/kb/profiling/

Well okay, one last check before the profiler: Use (time form) from the repl.








