# pert

Monte Carlo project estimation based on the US Navy's Program Evaluation and Review Technique
(PERT) approach.

## Quickstart

1. Install [leiningen]() (`brew install leiningen` via Homebrew on a Mac).
2. `lein repl`
3. Per the prompt which comes up, enter `(start-clerk!)` at the `user=>` prompt.
4. http://localhost:7777/notebooks/demo.clj will open in a browser.
5. Navigate to [notebooks/demo.clj](http://localhost:7777/notebooks/demo.clj) on the left.
6. Enjoy scrolling the report based on the example CSV project data at test/example.csv

Want to make your own? Grab your favorite spreadsheet program that generates CSV files[1] and
let's go.

Make a new spreadsheet with at least the following column headings (exactly):
- ID
- Title
- Description
- Dependencies
- Low
- Estimate
- High

You can find an example CSV file in this repository at test/example.csv.

> [1] Note that this "favorite program" can also be a script you write to extract this data from
> an existing project management data source. Excel, Calc, and Numbers all work fine, too.

### Task breakdown

Each row in the CSV file represents a subtask. The order of the rows is significant: you should
list your highest-priority tasks highest up. Project simulations will try executing tasks from
top to bottom, as long as task dependencies (see Dependencies below) are satisfied.

The Title field is a short name for the task. It can be whatever you want. The Description field
is a longer description of the task. Again, it can be whatever you want.

Tasks *do* need an ID. Alphanumeric IDs work well. Hard requirements: No whitespace, no commas.

### Three-point estimates

Each task uses a *three*-point estimate rather than a single number: a nominal, gut-feel estimate,
a wildly-optimistic low estimate, and a wildly-pessimistic high estimate. The gut-feel estimate
should go in the Estimate field. The wildly-optimistic estimate goes in the Low field, and the
wildly-pessimistic estimate goes in the High field.

The units for these estimates are Days. Decimal values are okay.

### Dependencies

List the IDs of the tasks that must complete before the given task in that task's Dependencies
field. When a task depends on multiple other tasks, separate the IDs of these dependencies by a
comma and any amount of whitespace (hence the need to keep commas and whitespace out of task IDs).

This can get tedious if your task breakdown is pretty serial with only a few different parallel
tracks. One trick to speed this up in a spreadsheet program is to copy and paste relevant ranges
of the ID column into the Dependencies column as a starting point.

The calculator works by simulating project schedules based on your estimates thousands of times.
As mentioned in the Three-point estimates section above, the simulator scans your tasks from top
to bottom in the order of your CSV file when looking for a free task, but it respects dependencies
by only picking up a free task if it has no incomplete dependencies.

### Number of workers

For purposes of estimation, this tool models workers pretty robotically. One worker = one task.
No pair programming, interruptions, Brooks' Law, etc.

Exploring the source code at notebooks/demo.clj shows some ways of running project simulations
with different numbers of workers. The demo report itself shows schedules for the example
project with one, two, and three workers.

## Rationale

> Measure twice, cut once.

Estimation was some of *this* programmer's most begrudging meta-work.

I based this estimation and scheduling calculator on the intersection of estimation techniques
asked for from various project managers over the course of my career.

I hope it helps you, but I built this for me, speeding up my own project estimates and what-if
scenarios.

### Uncertainty

How can you estimate how long it will take you to do something you've never done before?
To give a single number, even a best-guess, sets you up for failure.

So what do you say? It'll be ready when it's ready? We can do better.

The range of uncertainty around a tasks's best-guess duration can be fairly large. In general,
the larger the best-guess, the larger the range of the estimate's uncertainty.

Adding a wildly-optimistic low-ball and a wildly-pessimistic high-ball to a best-guess estimate
keeps us honest without sandbagging or overpromising. In spite of this transparency, you'll still
be asked for an estimated completion date. 

We could even take these three points and
model our task as a random variable based on them.

If we could just go off of that one range, we'd be done! If.

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

- PERT estimation breakdown at https://www.deepfriedbrainproject.com/2010/07/pert-formula.html
- Martin, Bob. [_The Clean Coder_](https://www.oreilly.com/library/view/clean-coder-the/9780132542913/). Chapter 10: "Estimation"
- [Apache Commons Math](https://commons.apache.org/proper/commons-math)
- [Clojure](https://www.clojure.org)
- [Clerk](https://github.com/nextjournal/clerk)
