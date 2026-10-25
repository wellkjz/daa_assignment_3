# Assignment 3 — Graph Algorithms

Selected language: **Java 17**.
Implementation tested and benchmark measured on **25 October 2026**.

## 1. Files and commands

The implementation is in `src/Java/Algorithms.java`. The supplied `Main.java`,
`Input.java`, `Json.java`, checker and workflow are retained. The manifest
contains `{"language":"Java"}`. The other language folders contain template stubs with explanatory comments
removed; their executable bodies are unchanged and they are not the submitted
implementation.

Requirements: JDK 17+ and Python 3.12+ on PATH. Python runs the checking tools;
the submitted graph algorithms run in Java. No package downloads are required.
Run from the repository root:

```sh
python tools/run.py --setup
python tools/run.py --check
python tests/test_solution.py
python tools/run.py --input tests/my_cases.jsonl
python -m unittest discover -s tools -p "test_*.py"
```

The launcher compiles using `javac --release 17 -encoding UTF-8` and runs
`java -Xmx384m -cp .build/Java Main`. To launch after compilation:

```sh
java -Xmx384m -cp .build/Java Main
```

Enter one JSON request per line; each produces one JSON result line.
`{"task":"ping"}` still returns `{"status":"ready"}`.
There are no prompts or debug lines on standard output.

Direct route-helper tests and benchmark:

```sh
javac --release 17 -encoding UTF-8 -cp .build/Java -d .build/Java tests/RouteTests.java tests/Benchmark.java
java -Xmx384m -cp .build/Java RouteTests
java -Xmx384m -cp .build/Java Benchmark > tests/benchmark_results.jsonl
```

## 2. Representation, limits and state reset

Input vertices and weights fit `int`. Costs and finite distances use `long`.
A simple path or forest has at most 99,999 edges, so even weights of magnitude
1,000,000,000 give a magnitude at most 99,999,000,000,000, safely within `long`.
Java promotes each weight to long when adding it to the long total or distance.
Comparators use `Integer.compare`.

The graph representation is `List<List<Integer>>`. Directed DFS adjacency lists
store neighbouring vertices. Prim and DAG relaxation adjacency lists store
**original edge IDs** so weighted and parallel edges remain distinguishable.
Sorting Kruskal's separate ID array does not change the original edge array. Nullable DAG distances use `Long[]`, and unreachable vertices have
`null` distance and parent edge `-1`.

Every mutable array, set, heap and DSU is allocated within its current call.
There is no mutable static algorithm state. The benchmark's volatile sink belongs
only to the standalone benchmark class. Repeated requests cannot inherit visited
flags, component counts or distances.

All graph traversals and DSU finds are iterative. DFS uses an `ArrayDeque`
stack and a persistent next-neighbour cursor for each vertex. Each
adjacency entry is examined once; resuming a suspended vertex does not rescan its
neighbours. The SCC transpose pass marks vertices when pushing, so a vertex is
pushed at most once. Deep chains do not consume Java's call stack.

In the complexity statements below, n is the vertex count, m the edge count,
q the number of unions, and L the number of vertices in a reconstructed route.
Memory includes returned results, but not a separate copy owned by the caller.

## 3. DSU

**Steps.** Initially each vertex is its own root, with size one. Find follows
parent links to a root and then compresses every traversed link to that root.
Union finds both roots; equal roots cause no change. Otherwise the smaller
tree is attached to the larger, sizes are added, the component count decreases
by one, and the largest size is updated. After every operation, the current
component count and maximum size are appended.

**Invariant and correctness.** A root represents exactly one connected component
of the processed undirected connections. Initially this is true for singleton
vertices. A connection within one component preserves the partition. A connection
between two different components merges exactly those components, preserving the
invariant. Root sizes count their vertices, so the reported largest size and
component count are correct after every operation. Path compression changes
only parent links inside a set and never changes its membership.

**Boundaries.** Self-unions and repeated unions leave counts unchanged. For n=0,
the initial largest value is zero and the returned states array is empty. No
initial state is emitted. Isolated vertices remain singleton components.

**Complexity.** O(n + q alpha(n)) amortised time, using path compression and
union by size; O(n+q) memory, including the q returned states.

## 4. Kruskal minimum spanning forest

**Steps.** Sort original edge IDs by weight. Java's stable object-array sort
keeps equal-weight IDs in their original input order.
Scan the sorted IDs and accept an edge precisely when DSU union succeeds.
Accumulate its weight in a long and retain its original ID. The final DSU count
includes isolated vertices.

**Invariant and correctness.** Chosen edges form a forest and can be extended to
a minimum spanning forest. Initially the empty forest satisfies this. If an
edge joins different DSU sets, it is a lightest available edge crossing a cut
between current components. By the cut/exchange property it can be included
without increasing the best attainable cost. Edges within a component would form
a cycle and are skipped. After all edges have been considered, endpoints of
every input edge are connected in the chosen forest; hence every input component
is spanned. The safe-edge invariant proves minimum total cost.

**Boundaries.** Loops fail union even when negatively weighted. Parallel and tied
edges are handled independently. Disconnected components are all spanned;
isolates are counted. Empty graphs return cost 0, no IDs, and 0 components.
A forest on n vertices and c components has n-c selected edges.

**Complexity.** O(n + m log(m+1)) time and O(n+m) memory. This includes the
ID array, sorting workspace, DSU and selected IDs.

## 5. Prim minimum spanning forest

**Steps.** Build undirected adjacency lists containing original edge IDs.
The min-heap stores entries `[weight, destination, edgeId]`. For each unvisited
start vertex, increment the component count and insert `[0,start,-1]`. This
special entry starts a tree without selecting an edge. Repeatedly remove the
minimum entry; skip it if its destination is already visited. Otherwise mark
that vertex, accept its incoming edge when edgeId is not -1, and insert its
edges to unvisited neighbours. Drain the heap and restart for the next component.

**Invariant and correctness.** After a component's initial entry is removed,
every queued edge has a visited origin. The heap contains all crossing edges
and may contain stale entries whose destinations are now visited. Skipping
these entries preserves all crossing candidates. The next accepted edge has
minimum weight across the current visited/unvisited cut, so the cut property
makes it safe. It adds one new vertex and cannot create a cycle. An empty heap
means that component is exhausted. Restarting produces a minimum spanning forest.

**Boundaries.** Loops never enter the heap because their other endpoint is
already visited. Parallel edges may enter, but obsolete entries are discarded.
Negative weights and ties are valid. An isolated restart contributes one component
and no edge. The heap is drained before the next restart.

**Complexity.** O(n + m log(m+1)) time with a lazy heap; O(n+m) memory.
Each non-loop edge is inserted at most once, when its first endpoint is visited.
The heap can contain O(m) entries, so claiming an indexed-heap bound for this
implementation would be incorrect.

## 6. Kosaraju strongly connected components

**Steps.** First run DFS on the original graph, collecting reverse finish order.
Build the transpose by adding u to the reverse adjacency list of v for every
original edge u -> v.
Process roots in reverse finish order, running a DFS on the transpose and giving
all vertices reached from each new root the same label. Finally scan original
edges, emitting each distinct pair of different component labels once.
A hash set stores keys such as `"3,8"` for pairs of component labels. The comma
separates the labels unambiguously. Labels are below 100,000, so keys have
bounded length under the published limits.

**Invariant and correctness.** For an edge between different SCCs A -> B, the
largest first-pass finish time in A exceeds the largest in B. Thus the next
unassigned SCC in decreasing finish order has no transpose path into another
unassigned SCC. Its transpose DFS reaches all its own vertices by strong
connectivity, and cannot add an unassigned vertex from another SCC. Previously
labelled vertices are skipped. Induction over these passes gives exactly the
SCC partition. The final original-edge scan emits precisely every cross-SCC
pair, while the hash set removes duplicates.

**Boundaries.** Self-loops stay internal, repeated cross edges produce one pair,
and isolates form separate SCCs. Empty input produces two empty arrays. Labels
are consecutive here, but correctness does not depend on particular numbers.

**Complexity.** O(n+m) expected time with hash-based deduplication, and O(n+m)
memory for both graph directions, stacks, labels and condensation.

## 7. Three-state DFS topological ordering

**Steps.** State 0 means unvisited, 1 active, and 2 finished. Traverse every
root using a stack. An edge to state 1 sets the DFS result's acyclic flag to
false; an edge to state 2 is harmless. Add a vertex to the order list when
it finishes, then reverse the completed list. The shared DFS returns both
this list and the flag. SCC uses the list even when cycles exist; topo returns
an empty order when the flag is false.

**Invariant and correctness.** Active vertices are precisely the current DFS
path. An edge to an active vertex closes a directed cycle. Conversely, a
directed cycle must expose an edge to an active vertex during DFS, so the
algorithm detects every cycle. In an acyclic graph, for every edge u -> v,
v finishes before u: either DFS explores v from u or v has already finished.
Reversing finish order therefore puts u before v. Every vertex is started and
finished once, including vertices in other components.

**Boundaries.** A self-loop sets the acyclic flag to false. Duplicate edges to a finished
vertex do not create a false cycle. A cycle in any component causes false and
an empty order. For n=0, the result is true and an empty order.

**Complexity.** O(n+m) time and memory. Next-neighbour cursors ensure linear
edge scanning even for high-degree vertices.

## 8. DAG shortest paths and route reconstruction

**Steps.** Run the same cycle-detecting DFS on the **whole** directed graph.
If any cycle exists, return false and two empty arrays. Otherwise initialise
the source to distance 0 and all other distances to null; all parents start
at -1. Traverse topological order. Skip unreachable vertices and relax every
outgoing edge from each reachable vertex. On a strict improvement, record the
new distance and that original edge ID.

**Invariant and correctness.** When a vertex is processed, all its predecessors
have already been processed, so all possible final edges of a source-to-vertex
path have been considered. Induction over topological order proves that its
stored distance is the minimum path cost, or null if no such path exists.
Negative edges do not invalidate this argument because the graph has no cycles.
Every selected parent edge certifies an equality
distance[parent] + weight = distance[vertex]. Parent edges move backwards in
topological order; therefore their chain terminates at the source and cannot
cycle. A reachable incoming path back to the source would itself be a cycle,
so the source keeps distance 0 and parent -1.

**Boundaries.** A disconnected cycle still rejects the request. A finite distance
of -1 is valid and is distinct from null. Ties keep the first selected parent;
different optimal parents are acceptable. Parallel edges are distinguished by
ID. A one-vertex edgeless graph returns [0] and [-1].

**Complexity.** O(n+m) time and memory, including topological ordering and output.

**Helper.** `reconstructRoute(source,target,edges,parentEdge)` follows original
edge IDs backwards, appends vertices and reverses the result. It is called only
on an acyclic DAG result. The source route is [source]; an unreachable target
returns []. A direction check and a step bound reject malformed parent chains.
For valid results it takes O(L) time and output space; no n-sized visited array
is allocated by the helper.

Actual calls from `tests/RouteTests.java` and results in
`tests/route_actual.jsonl`:

For `edges = [[0,1,3],[0,2,8],[1,2,-7],[2,3,4]]`,
`source=0`, `parentEdge=[-1,0,2,3,-1]`:

| Call | Vertex route | Selected edge IDs | Cost |
|---|---|---|---:|
| reconstructRoute(0,3,edges,parentEdge) | [0,1,2,3] | [0,2,3] | 0 |
| reconstructRoute(0,0,edges,parentEdge) | [0] | [] | 0 |
| reconstructRoute(0,4,edges,parentEdge) | [] | [] | no route |

For `edges = [[0,1,8],[0,1,2],[1,2,-1],[0,2,1]]`,
the calculated parents are `[-1,1,3,-1]`:

| Call | Vertex route | Selected edge IDs | Cost |
|---|---|---|---:|
| reconstructRoute(0,1,edges,parentEdge) | [0,1] | [1] | 2 |
| reconstructRoute(0,2,edges,parentEdge) | [0,2] | [3] | 1 |

Using the equally valid tied parent array `[-1,1,2,-1]` reconstructs
[0,1,2] with IDs [1,2] and cost 2+(-1)=1. The test also asserts this.
For each emitted route, the test checks edge direction and total weight
against the calculated shortest distance.

## 9. Validation actually performed

- Setup ping: PASS.
- All **36 public cases**: PASS.
- Two complete public batches in one JVM: PASS.
- **562 own small cases**, including seeded random cases with independent
  reference answers: PASS.
- Those cases, their reverse order, and a repeat in one JVM:
  **1,686 mixed requests**: PASS.
- **10 validator assertions**: accepts alternative valid forests, SCC labels,
  topological orders and DAG parents; rejects invalid witnesses: PASS.
- **9 full-limit cases**, each with n=100,000 and q or m=250,000: PASS.
- Direct route-helper calls, a valid alternative tie and malformed parent cycle:
  PASS.

Own test input, independently computed expected answers, actual results,
generator and commands are in `tests/`. See `tests/README.md` for
independent oracle methods and full-limit justifications. Large tests include
a 100,000-vertex DFS chain, one giant SCC, deep cycles, sums of magnitude
99,999,000,000,000 and a cycle unreachable from the source.

## 10. Kruskal-Prim benchmark

### Reproduction and environment

Run the build and benchmark commands from Section 1. The complete generator
and timing code are in `tests/Benchmark.java`; the raw measurements are in
`tests/benchmark_results.jsonl`.

Hardware: 12th Gen Intel Core i5-12450HX; 12 logical processors available.
OS: Windows 11 10.0. Runtime: OpenJDK 17.0.18+8, OpenJDK 64-Bit Server VM.
Maximum heap: 384 MiB (`-Xmx384m`). Python checker: 3.12.14.
Timer: `System.nanoTime()`; elapsed nanoseconds are converted to milliseconds.

For each size, the generator creates one edge array and both methods receive
that same unchanged array. It uses `java.util.Random(20261025L + n)`.
The first n-1 edges form the path 0-1-...-(n-1); each weight is generated as
`-1 - random.nextInt(1000000)`. Remaining edges have two random endpoints
and weights from `random.nextInt(1000001)`. Thus the path has strictly
negative weights and every other edge is nonnegative. Loops and parallel
edges are allowed. The generator and seed reproduce the complete input.

### Independent correctness check

The negative path is the unique minimum spanning tree. It already connects
all vertices using n-1 edges. Any different spanning tree must remove at
least one strictly negative path edge and replace it with a nonnegative edge,
increasing the total weight. The same argument applies if several edges change.

Before timing, the harness verifies that each method returns one component
and exactly the n-1 distinct path edge IDs, with their correct summed weight.
This independently verifies feasibility and optimum cost; it does not merely
check whether the two methods agree.

### Timing procedure

Each method runs five warm-up calls per size; those timings are discarded.
Five further calls per method are recorded, alternating which method runs
first in each measured round. The returned cost is consumed and checked
after the timed interval.

The interval includes the full core algorithm call on an already constructed
int[][]: sorting IDs or building adjacency lists, DSU or priority-queue work,
and result creation. It excludes compilation, JVM startup, graph generation,
JSON parsing, input conversion, validation and printing. Allocation and any
garbage collection during the algorithm call are included; no explicit GC
is requested. Both methods use the same measurement boundary.

### Measurements in milliseconds

| n | m | Seed | Method | Run 1 | Run 2 | Run 3 | Run 4 | Run 5 | Median |
|---:|---:|---:|---|---:|---:|---:|---:|---:|---:|
| 1,000 | 2,500 | 20262025 | Kruskal | 0.8430 | 0.8388 | 0.8223 | 0.8570 | 0.8197 | 0.8388 |
| 1,000 | 2,500 | 20262025 | Prim | 1.3541 | 0.9190 | 0.8758 | 0.8334 | 0.6805 | 0.8758 |
| 10,000 | 25,000 | 20271025 | Kruskal | 13.4677 | 15.6678 | 12.7433 | 13.2163 | 14.1852 | 13.4677 |
| 10,000 | 25,000 | 20271025 | Prim | 6.0370 | 6.7804 | 6.0675 | 6.2150 | 6.2959 | 6.2150 |
| 100,000 | 250,000 | 20361025 | Kruskal | 95.5887 | 94.2026 | 95.6383 | 108.2271 | 99.9553 | 95.6383 |
| 100,000 | 250,000 | 20361025 | Prim | 90.8927 | 130.0062 | 122.2456 | 118.2747 | 112.0235 | 118.2747 |

Both methods returned one component and the independently known optimum:

- n=1,000: **-490,195,159**.
- n=10,000: **-4,974,966,137**.
- n=100,000: **-49,976,151,936**.

### Interpretation and limitations

Kruskal had the lower median at 1,000 and 100,000 vertices; Prim had the lower
median at 10,000 vertices. These results describe the measured executions
rather than a universal ranking of the algorithms. Both implementations
have O(n + m log(m+1)) time bounds and O(n+m) memory bounds.

The graph family is intentionally simple so its exact optimum is easy to
prove: a negative path with nonnegative extra edges. It is a restricted
family of connected sparse multigraphs and does not represent all graphs.
It may favour particular traversal and heap behaviour. Tests of disconnected
graphs, arbitrary small weighted graphs and other edge cases are separate.
Five measurements on one graph per size and one computer are limited
evidence; JIT compilation, garbage collection, cache effects and scheduling
can cause variability.

## 11. Sources and AI acknowledgement

Primary source: [RZhaniya/daa-assignment-3-graphs](https://github.com/RZhaniya/daa-assignment-3-graphs),
its English README, published contracts, public cases and assignment PDF.
The downloaded template tree was
`76c1d91daa90ceadcb225f5408d54d4bfcf397f9`.
The supplied JSON adapters, runner, validators, workflow and other-language stubs
originate from that template. Original specification PDFs remain available
through its documentation links.

**AI assistance:** ChatGPT/Codex generated and simplified the implementation,
own tests, benchmark harness and analysis, and performed the recorded local
checks and measurements. The evidence includes independent small-graph oracles,
semantic witness checks and full-limit cases.
