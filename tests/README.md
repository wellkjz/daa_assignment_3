# Own tests and evidence

Run from the repository root with JDK 17+ and Python 3.12+:

```sh
python tests/test_solution.py
```

This compiles the selected Java solution, regenerates inputs and expected answers,
executes the real JSON Lines adapter, checks every result, calls the Java route
helper directly, and stores actual outputs. Seed: **20261023**.

## Files

| File | Purpose |
|---|---|
| test_solution.py | Deterministic small, random, mixed-request and full-limit test generator/runner |
| my_cases.jsonl | 562 small input requests |
| expected.jsonl | Independent expected answers, in the same order |
| actual.jsonl | Actual Java outputs, in the same order |
| limits/*_input.jsonl | Nine generated full-limit inputs, one file per case |
| limits/*_expected.jsonl | Independently justified expected full-limit results |
| limits/*_actual.jsonl | Actual full-limit results |
| validator_results.json | Alternative-valid and invalid witness examples, with assertion outcomes |
| RouteTests.java | Direct helper calls, direction/cost checks and malformed-parent-cycle rejection |
| route_actual.jsonl | Five actual route outputs |
| results.txt | Own-test summary |
| public_results.txt | Captured local setup/public-check output |
| Benchmark.java | Generator with a known optimum and timing harness |
| benchmark_results.jsonl | Environment, graph seeds, five measurements and medians |

For individual inputs:

```sh
python tools/run.py --input tests/my_cases.jsonl
```

That command prints results only. Use the test script for semantic comparison.
The limits folder contains all nine stress cases; the own runner executes
each case with the template's 20-second per-batch timeout and 384 MiB Java heap.

## Independent expected answers

The first 28 requests are explicit boundary examples. They include ping, empty
graphs, isolates, repeated/self unions, negative loops, unequal parallel edges,
tied forests, sums beyond 32 bits, SCC duplicate condensation edges, edges to
finished DFS vertices, self-cycles, cycles in other components, nonzero sources,
unreachable vertices, and tied shortest paths.

The remaining 534 requests are generated in 80 seeded rounds. For small cases:

- DSU uses direct whole-array component relabelling and counting.
- MST enumerates all subsets of size n-c, rejecting cycles and non-spanning
  candidates, and takes the minimum cost. Connectivity comes from transitive
  closure. This is an exhaustive oracle, independent of Kruskal and Prim.
- SCC uses Floyd-style Boolean transitive closure: two vertices share a label
  exactly when both reach each other.
- Topological acyclicity uses Kahn's indegree queue, not DFS.
- DAG distances use Bellman-Ford on graphs first checked for acyclicity by Kahn.

The supplied semantic checker checks the Java witnesses against these reference
facts. It checks forest edges and optimum cost, SCC partitions and exact
condensation, topological precedence, and distances plus parent chains.
It does not require identical non-unique answers. The checker is also exercised
with explicitly different valid answers and deliberately invalid answers for
both MST methods, SCC, topo and DAG (10 assertions).

All 562 inputs, then their reverse order, then the original order again run
through **one Java process** (1,686 requests), detecting stale graph state.

## Full-limit cases and mathematical expectations

Every case has n=100,000 and q or m=250,000.

| Case | Construction and independently justified expected result |
|---|---|
| DSU | Join consecutive vertices of a path, then repeat (0,n-1). After path union i (zero based), components=n-i-1 and largest=i+2; all later states are [1,n]. |
| Kruskal | Positive path edges of weight 1e9 plus negative self-loops. Loops cannot be selected; every path edge is forced. Cost=(n-1)*1e9=99,999,000,000,000, components=1. |
| Prim | Exactly the same path and loops, with the same forced optimum. |
| SCC chain | Directed path plus duplicate forward edges. No reverse path exists, so every vertex is a separate SCC; distinct condensation pairs are exactly the path edges. |
| Topo chain | Same directed path and duplicates. The path forces order 0,1,...,n-1. |
| DAG chain | Same forward edges, each weight -1e9, source 0. Every path step contributes -1e9, so distance[v]=-v*1e9. Parallel copies may use alternative valid IDs. |
| Giant SCC | Replace one duplicate edge by n-1 -> 0. The path and return edge make every vertex mutually reachable: one label, no condensation edges. |
| Topo cycle | The same return edge creates a directed cycle: false and empty order. |
| Unreachable DAG cycle | Source 0 is isolated; vertices 1..n-1 form a directed cycle plus duplicate edges. Whole-graph validation must return false and empty distance/parent arrays. |

The full-limit data are stored as ordinary UTF-8 JSONL for inspection and are
reproducible by the generator; the deliverable ZIP compresses them efficiently.

## Route and benchmark commands

After any solution compilation:

```sh
javac --release 17 -encoding UTF-8 -cp .build/Java -d .build/Java tests/RouteTests.java tests/Benchmark.java
java -Xmx384m -cp .build/Java RouteTests
java -Xmx384m -cp .build/Java Benchmark > tests/benchmark_results.jsonl
```

Route expected answers and all benchmark settings are explained in ANALYSIS.md.

