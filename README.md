# pert

Monte Carlo project estimation based on the US Navy's Program Evaluation and Review Technique
(PERT) approach.

## Estimates

> Measure twice, cut once.

Estimation is a programmer's most begrudging meta-work. Estimation was some of *this*
programmer's most begrudging meta-work.

I wrote this tool as an estimation and scheduling calculator based on the intersection of
estimation techniques asked for from various team and project managers over the course of my
career.

The tool is meant to save labor and illustrate nuance in project planning assumptions in
general and for a given project in particular.

### Uncertainty

The answer to "how long will this take?" is not a single number for most software tasks. Most
new software projects involve unfamiliar project domains and technologies.

Programmers can often offer a best-guess at a task duration, but the range of uncertainty
around this best guess can be fairly large. In general, the larger the best-guess, the larger
the range of the estimate's uncertainty.

Capturing and communicating this uncertainty can be challenging and amplify frustrations. In
particular, it can be unclear when the requested estimate is a best-guess and when the request
is for a duration which will complete the task with 99% certainty--these are not the same.

### Task Breakdown

We break down larger projects in order to coordinate scope, divide work, and predict the project's
evolution.

Since uncertainty ranges correlate with best-guess estimates, estimating a combination of small
tasks should reach a more reliable estimate than estimating the same scope of work as a single task.

Treating estimates as a single number can lead to systematic optimism in project schedules. If our
task estimates indeed have negligible uncertainty around our best guesses, then we can be quite
comfortable treating the sum of estimates for a series of tasks as our estimate for the series as a
whole. That usually is not the case--our individual task estimates may themselves have a large
confidence range. Neglecting these ranges when combining task estimates is sure to have us either
quite optimistic or wildly sandbagging. Neither of these leaves us with a useful, defensible,
predictive estimate.

### Dependencies

Once you break a task down into smaller subtasks you immediately see dependencies among them. In
something as simple as getting dressed, one must adorn underpants before pants, socks before shoes.

When delivering a project as a solo developer, you must respect dependencies in your plan as you go.
When staffing a project, the task dependencies can help you see at which point adding additional
developers buys no earlier of an expected completion time.

## Defensible estimates

> Garbage in, garbage out.

My goals for this tool are as follows:
- Its inputs must be only those I am confident in providing myself or comfortable asking for.
- It must be easy to explain how the inputs feed into the overall project estimate.
- It must be easy enough to use that I can recalculate several estimates as the project progresses
  or scope is renegotiated.

### Inputs

We're breaking our project down into tasks. For each task, we'll need task dependencies and
estimates. The dependencies are a list of tasks which must complete before a given task can start.
Task estimates use three numbers to define a probability range: A wildly optimistic duration, a
best-guess duration, and a wildly pessimistic duration.

Every project estimate I have been asked for has involved a task breakdown with dependencies. Every
estimate I have given has had a confidence range. The key here is that we are capturing the
confidence range.

You may have a spreadsheet already containing much of this input. Use a column named "ID" to choose
a non-whitespace unique ID for a task. In a "Dependencies" column, add a comma-separated list of the
IDs of tasks the task dependes on. Use a "Title" column to give the task a short name. You may
already have a column named "Estimate" with your best-guess estimate in days. Add a "Low" column for
a wildly-optimistic estimate in days, and "High" for a wildly-pessimistic task estimate in days.
Sort the rows by the priority of the tasks.

Save it as CSV. That's it.

### Aggregation

Optimistic, best-guess, and pessimistic estimates are used to define a Beta distribution for each
task. The Beta distribution can be sampled to get a duration for that task. We use the probability
distributions defining the individual task estimates to simulate the project 10,000 times for a
given number of team members assigned to the project. These simulations let us know not only when
we can expect the full project to finish, but also when our inputs predict the completion of each
task.

### Tracking

- Estimation error
- Scope renegotiation
- Discovered unknowns
- Staffing changes and contingencies

## Usage

# References

PERT estimation. Looks like there's a nice free series on it to be found at
https://www.deepfriedbrainproject.com/2010/07/pert-formula.html

Martin, Bob. _The Clean Coder_. Chapter 10: "Estimation"

Apache Commons Math

Clojure

Clerk
