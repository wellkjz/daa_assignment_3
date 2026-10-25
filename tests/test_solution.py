
import itertools
import json
import pathlib
import random
import subprocess
import sys
from collections import deque

ROOT = pathlib.Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "tools"))
from run import build, execute
from public_validate import check

OUT = ROOT / "tests"
SEED = 20261023

def closure(n, edges, undirected=False):
    reach = [[i == j for j in range(n)] for i in range(n)]
    for edge in edges:
        a, b = edge[:2]
        reach[a][b] = True
        if undirected:
            reach[b][a] = True
    for k in range(n):
        for i in range(n):
            for j in range(n):
                reach[i][j] |= reach[i][k] and reach[k][j]
    return reach

def oracle(x):


    n, task = x.get("n", 0), x["task"]
    if task == "ping":
        return {"status": "ready"}
    if task == "dsu":
        labels, states = list(range(n)), []
        for a, b in x["operations"]:
            old, new = labels[b], labels[a]
            labels = [new if item == old else item for item in labels]
            states.append([len(set(labels)), max((labels.count(v) for v in set(labels)), default=0)])
        return {"states": states}
    edges = x["edges"]
    if task in ("kruskal", "prim"):
        reach = closure(n, edges, True)
        components = len({min(j for j in range(n) if reach[i][j]) for i in range(n)})
        best = None
        best_ids = []
        for ids in itertools.combinations(range(len(edges)), n - components):
            labels = list(range(n))
            cost = 0
            for edge_id in ids:
                a, b, weight = edges[edge_id]
                if labels[a] == labels[b]:
                    break
                old, new = labels[b], labels[a]
                labels = [new if item == old else item for item in labels]
                cost += weight
            else:
                if all(labels[a] == labels[b] for a, b, _ in edges):
                    if best is None or cost < best:
                        best, best_ids = cost, list(ids)
        return {"cost": best, "components": components, "edge_ids": best_ids}
    if task == "scc":
        reach = closure(n, edges)
        labels = [min(j for j in range(n) if reach[i][j] and reach[j][i]) for i in range(n)]
        pairs = sorted({(labels[a], labels[b]) for a, b in edges if labels[a] != labels[b]})
        return {"component": labels, "condensation": [list(p) for p in pairs]}
    indegree, adj = [0] * n, [[] for _ in range(n)]
    for edge in edges:
        a, b = edge[:2]
        indegree[b] += 1
        adj[a].append(b)
    queue = deque(i for i in range(n) if not indegree[i])
    order = []
    while queue:
        a = queue.popleft()
        order.append(a)
        for b in adj[a]:
            indegree[b] -= 1
            if not indegree[b]:
                queue.append(b)
    acyclic = len(order) == n
    if task == "topo":
        return {"acyclic": acyclic, "order": order if acyclic else []}
    if not acyclic:
        return {"acyclic": False, "distance": [], "parent_edge": []}
    distance, parent = [None] * n, [-1] * n
    distance[x["source"]] = 0
    for _ in range(n - 1):
        changed = False
        for edge_id, (a, b, weight) in enumerate(edges):
            if distance[a] is not None and (distance[b] is None or distance[a] + weight < distance[b]):
                distance[b], parent[b], changed = distance[a] + weight, edge_id, True
        if not changed:
            break
    return {"acyclic": True, "distance": distance, "parent_edge": parent}

def write_jsonl(path, items):
    with path.open("w", encoding="utf-8", newline="\n") as stream:
        for item in items:
            stream.write(json.dumps(item, separators=(",", ":")) + "\n")

def small_cases():
    cases = [{"task": "ping"}]
    for task in ("dsu", "kruskal", "prim", "scc", "topo"):
        cases.append({"task": task, "n": 0, "operations" if task == "dsu" else "edges": []})
    cases += [
        {"task": "dsu", "n": 6, "operations": [[0,0],[0,1],[1,0],[2,3],[1,3],[5,5],[4,5]]},
        {"task": "scc", "n": 6, "edges": [[0,1],[1,0],[1,2],[0,2],[1,2],[2,3],[3,2],[3,3],[4,4]]},
        {"task": "topo", "n": 5, "edges": [[0,2],[1,2],[2,3],[0,3],[0,2]]},
        {"task": "topo", "n": 5, "edges": [[0,1],[3,4],[4,3]]},
        {"task": "topo", "n": 1, "edges": [[0,0]]},
        {"task": "dag", "n": 1, "source": 0, "edges": []},
        {"task": "dag", "n": 5, "source": 0, "edges": [[0,1,3],[0,2,8],[1,2,-7],[2,3,4]]},
        {"task": "dag", "n": 5, "source": 0, "edges": [[0,1,1],[3,4,-5],[4,3,2]]},
        {"task": "dag", "n": 1, "source": 0, "edges": [[0,0,0]]},
        {"task": "dag", "n": 4, "source": 0, "edges": [[0,1,8],[0,1,2],[1,2,-1],[0,2,1]]},
        {"task": "dag", "n": 5, "source": 3, "edges": [[3,2,-1],[2,1,-1000000000],[1,0,-1000000000]]},
        {"task": "dag", "n": 5, "source": 0, "edges": [[0,1,1000000000],[1,2,1000000000],[2,3,1000000000]]},
    ]
    for task in ("kruskal", "prim"):
        for n, edges in [
            (5, []),
            (6, [[0,0,-100],[0,1,5],[0,1,-2],[1,2,0],[0,2,4],[3,4,-7]]),
            (4, [[0,1,1],[1,2,1],[0,2,1],[2,3,1],[1,3,1]]),
            (4, [[0,1,1000000000],[1,2,1000000000],[2,3,1000000000]]),
            (4, [[0,1,-1000000000],[1,2,-1000000000],[2,3,-1000000000]])
        ]:
            cases.append({"task": task, "n": n, "edges": edges})
    return cases

def random_cases():
    rng = random.Random(SEED)
    cases = []
    for _ in range(80):
        n = rng.randint(0, 7)
        pairs = [[rng.randrange(n), rng.randrange(n)] for _ in range(rng.randint(0, 10))] if n else []
        weighted = [pair + [rng.randint(-10, 10)] for pair in pairs]
        cases += [{"task": "dsu", "n": n, "operations": pairs}]
        for task in ("kruskal", "prim"):
            cases.append({"task": task, "n": n, "edges": weighted})
        for task in ("scc", "topo"):
            cases.append({"task": task, "n": n, "edges": pairs})
        if n:
            cases.append({"task": "dag", "n": n, "source": rng.randrange(n), "edges": weighted})
            permutation = list(range(n))
            rng.shuffle(permutation)
            edges = [[permutation[a], permutation[b], rng.randint(-10,10)]
                     for a in range(n) for b in range(a+1,n) if rng.random() < .3]
            cases.append({"task": "dag", "n": n, "source": rng.randrange(n), "edges": edges})
    return cases

def full_cases():
    n, m = 100000, 250000
    chain = [[i, i+1] for i in range(n-1)]
    operations = chain + [[0, n-1] for _ in range(m-len(chain))]
    states = [[n-i-1, i+2] for i in range(n-1)] + [[1,n] for _ in range(m-n+1)]
    yield "DSU full limits", {"task":"dsu","n":n,"operations":operations}, {"states":states}
    weighted = [edge + [1000000000] for edge in chain]
    weighted += [[i % n, i % n, -1000000000] for i in range(m-len(weighted))]
    for task in ("kruskal","prim"):
        yield task + " full limits", {"task":task,"n":n,"edges":weighted}, {
            "cost":(n-1)*1000000000,"components":1,"edge_ids":list(range(n-1))}
    directed = chain + [chain[i % len(chain)] for i in range(m-len(chain))]
    yield "SCC deep chain/full limits", {"task":"scc","n":n,"edges":directed}, {
        "component":list(range(n)), "condensation":chain}
    yield "topo deep chain/full limits", {"task":"topo","n":n,"edges":directed}, {
        "acyclic":True,"order":list(range(n))}
    negative = [edge + [-1000000000] for edge in directed]
    yield "DAG deep chain/full limits", {"task":"dag","n":n,"source":0,"edges":negative}, {
        "acyclic":True,"distance":[-i*1000000000 for i in range(n)],"parent_edge":[-1]+list(range(n-1))}
    cyclic = directed.copy()
    cyclic[-1] = [n-1, 0]
    yield "SCC giant component", {"task":"scc","n":n,"edges":cyclic}, {
        "component":[0]*n,"condensation":[]}
    yield "topo deep cycle", {"task":"topo","n":n,"edges":cyclic}, {
        "acyclic":False,"order":[]}
    away = [[i,i+1,1] for i in range(1,n-1)] + [[n-1,1,1]]
    away += [[1,2,1] for _ in range(m-len(away))]
    yield "DAG full cycle unreachable from source", {"task":"dag","n":n,"source":0,"edges":away}, {
        "acyclic":False,"distance":[],"parent_edge":[]}

def validator_tests():
    examples = []
    for task in ("kruskal","prim"):
        request = {"task":task,"n":3,"edges":[[0,1,1],[1,2,1],[0,2,1]]}
        examples.append((task, request, {"cost":2,"components":1,"edge_ids":[0,2]},
                         {"cost":2,"components":1,"edge_ids":[0,0]}))
    examples += [
        ("scc", {"task":"scc","n":3,"edges":[[0,1],[1,0],[1,2]]},
         {"component":[7,7,19],"condensation":[[7,19]]},
         {"component":[7,7,19],"condensation":[]}),
        ("topo", {"task":"topo","n":3,"edges":[[0,2],[1,2]]},
         {"acyclic":True,"order":[1,0,2]}, {"acyclic":True,"order":[0,2,1]}),
        ("dag", {"task":"dag","n":3,"source":0,"edges":[[0,1,2],[0,1,2],[1,2,-1],[0,2,1]]},
         {"acyclic":True,"distance":[0,2,1],"parent_edge":[-1,1,3]},
         {"acyclic":True,"distance":[0,2,1],"parent_edge":[-1,2,3]})
    ]
    evidence = []
    for name, request, good, bad in examples:
        expected = oracle(request)
        assert check(request, good, expected), name + " alternative rejected"
        assert not check(request, bad, expected), name + " invalid accepted"
        evidence.append({"task":name,"input":request,"expected":expected,"alternative_valid":good,
                         "invalid":bad,"accepted_alternative":True,"rejected_invalid":True})
    (OUT/"validator_results.json").write_text(json.dumps(evidence,indent=2)+"\n",encoding="utf-8")
    return len(evidence)*2

def main():
    command = build("Java")
    log = []
    cases = small_cases() + random_cases()
    expected = [oracle(case) for case in cases]
    actual = execute(command, cases + list(reversed(cases)) + cases)
    assert len(actual) == 3*len(cases)
    for index, (request, want, got) in enumerate(zip(
            cases+list(reversed(cases))+cases, expected+list(reversed(expected))+expected, actual)):
        assert check(request, got, want), f"mixed case {index}: {request}, {got}, {want}"
    write_jsonl(OUT/"my_cases.jsonl", cases)
    write_jsonl(OUT/"expected.jsonl", expected)
    write_jsonl(OUT/"actual.jsonl", actual[:len(cases)])
    log.append(f"PASS {len(cases)} small cases; {3*len(cases)} mixed requests in one JVM (seed {SEED})")
    validations = validator_tests()
    log.append(f"PASS {validations} alternative-valid / invalid-witness assertions")
    limits = OUT / "limits"
    limits.mkdir(exist_ok=True)
    for index, (name, request, want) in enumerate(full_cases(), 1):
        got = execute(command, [request])[0]
        assert check(request, got, want), name
        for suffix, item in (("input",request),("expected",want),("actual",got)):
            write_jsonl(limits / f"{index:02d}_{request['task']}_{suffix}.jsonl", [item])
        message = f"PASS {name}: n={request['n']}, count={len(request.get('edges',request.get('operations',[])))}"
        log.append(message)
        print(message, flush=True)
    subprocess.run(["javac","--release","17","-encoding","UTF-8","-cp",str(ROOT/".build/Java"),
                    "-d",str(ROOT/".build/Java"),str(OUT/"RouteTests.java")], check=True)
    result = subprocess.run(["java","-Xmx384m","-cp",str(ROOT/".build/Java"),"RouteTests"],
                            capture_output=True,text=True,encoding="utf-8",check=True)
    (OUT/"route_actual.jsonl").write_text(result.stdout,encoding="utf-8")
    log.append("PASS route helper: 5 routes + tied alternative parents + invalid parent cycle")
    (OUT/"results.txt").write_text("\n".join(log)+"\n",encoding="utf-8")
    print("\n".join(log[:2]+log[-1:]), flush=True)

if __name__ == "__main__":
    main()
