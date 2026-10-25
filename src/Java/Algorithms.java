import java.util.*;

public class Algorithms {
    private static class DSU {
        int[] parent;
        int[] size;
        int components;
        int largest;

        DSU(int n) {
            parent = new int[n];
            size = new int[n];
            components = n;
            largest = 0;

            if (n > 0) {
                largest = 1;
            }

            for (int v = 0; v < n; v++) {
                parent[v] = v;
                size[v] = 1;
            }
        }

        int find(int v) {
            int root = v;

            while (parent[root] != root) {
                root = parent[root];
            }

            while (v != root) {
                int next = parent[v];
                parent[v] = root;
                v = next;
            }

            return root;
        }

        boolean union(int u, int v) {
            int a = find(u);
            int b = find(v);

            if (a == b) {
                return false;
            }

            if (size[a] < size[b]) {
                int temp = a;
                a = b;
                b = temp;
            }

            parent[b] = a;
            size[a] += size[b];
            components--;
            largest = Math.max(largest, size[a]);
            return true;
        }
    }

    static Map<String, Object> dsu(Map<String, Object> input) {
        int n = Input.vertexCount(input);
        int[][] operations = Input.operations(input);
        DSU sets = new DSU(n);
        int[][] states = new int[operations.length][2];

        for (int i = 0; i < operations.length; i++) {
            sets.union(operations[i][0], operations[i][1]);
            states[i][0] = sets.components;
            states[i][1] = sets.largest;
        }

        return Map.of("states", states);
    }

    static Map<String, Object> kruskal(Map<String, Object> input) {
        return kruskal(Input.vertexCount(input), Input.edges(input));
    }

    static Map<String, Object> kruskal(int n, int[][] edges) {
        Integer[] ids = new Integer[edges.length];

        for (int i = 0; i < edges.length; i++) {
            ids[i] = i;
        }

        Arrays.sort(ids, (a, b) -> Integer.compare(edges[a][2], edges[b][2]));

        DSU sets = new DSU(n);
        List<Integer> selected = new ArrayList<>();
        long cost = 0;

        for (int id : ids) {
            int u = edges[id][0];
            int v = edges[id][1];
            int weight = edges[id][2];

            if (sets.union(u, v)) {
                selected.add(id);
                cost += weight;
            }
        }

        return Map.of("cost", cost, "edge_ids", selected, "components", sets.components);
    }

    private static List<List<Integer>> emptyGraph(int n) {
        List<List<Integer>> graph = new ArrayList<>();

        for (int v = 0; v < n; v++) {
            graph.add(new ArrayList<>());
        }

        return graph;
    }

    private static List<List<Integer>> directedGraph(int n, int[][] edges) {
        List<List<Integer>> graph = emptyGraph(n);

        for (int[] edge : edges) {
            graph.get(edge[0]).add(edge[1]);
        }

        return graph;
    }

    static Map<String, Object> prim(Map<String, Object> input) {
        return prim(Input.vertexCount(input), Input.edges(input));
    }

    static Map<String, Object> prim(int n, int[][] edges) {
        List<List<Integer>> graph = emptyGraph(n);

        for (int id = 0; id < edges.length; id++) {
            graph.get(edges[id][0]).add(id);
            graph.get(edges[id][1]).add(id);
        }

        PriorityQueue<int[]> queue =
                new PriorityQueue<>((a, b) -> Integer.compare(a[0], b[0]));
        boolean[] visited = new boolean[n];
        List<Integer> selected = new ArrayList<>();
        long cost = 0;
        int components = 0;

        for (int start = 0; start < n; start++) {
            if (visited[start]) {
                continue;
            }

            components++;
            queue.add(new int[]{0, start, -1});

            while (!queue.isEmpty()) {
                int[] item = queue.remove();
                int weight = item[0];
                int v = item[1];
                int edgeId = item[2];

                if (visited[v]) {
                    continue;
                }

                visited[v] = true;

                if (edgeId != -1) {
                    selected.add(edgeId);
                    cost += weight;
                }

                for (int id : graph.get(v)) {
                    int to = edges[id][0];

                    if (to == v) {
                        to = edges[id][1];
                    }

                    if (!visited[to]) {
                        queue.add(new int[]{edges[id][2], to, id});
                    }
                }
            }
        }

        return Map.of("cost", cost, "edge_ids", selected, "components", components);
    }

    private static class DfsResult {
        boolean acyclic = true;
        List<Integer> order = new ArrayList<>();
    }

    private static DfsResult dfs(List<List<Integer>> graph) {
        int n = graph.size();
        int[] state = new int[n];
        int[] nextEdge = new int[n];
        Deque<Integer> stack = new ArrayDeque<>();
        DfsResult result = new DfsResult();

        for (int start = 0; start < n; start++) {
            if (state[start] != 0) {
                continue;
            }

            state[start] = 1;
            stack.push(start);

            while (!stack.isEmpty()) {
                int v = stack.peek();

                if (nextEdge[v] == graph.get(v).size()) {
                    stack.pop();
                    state[v] = 2;
                    result.order.add(v);
                    continue;
                }

                int to = graph.get(v).get(nextEdge[v]);
                nextEdge[v]++;

                if (state[to] == 1) {
                    result.acyclic = false;
                } else if (state[to] == 0) {
                    state[to] = 1;
                    stack.push(to);
                }
            }
        }

        Collections.reverse(result.order);
        return result;
    }

    static Map<String, Object> scc(Map<String, Object> input) {
        int n = Input.vertexCount(input);
        int[][] edges = Input.edges(input);
        List<List<Integer>> graph = directedGraph(n, edges);
        List<List<Integer>> reverse = emptyGraph(n);

        for (int[] edge : edges) {
            reverse.get(edge[1]).add(edge[0]);
        }

        List<Integer> order = dfs(graph).order;
        int[] component = new int[n];
        Arrays.fill(component, -1);
        Deque<Integer> stack = new ArrayDeque<>();
        int label = 0;

        for (int start : order) {
            if (component[start] != -1) {
                continue;
            }

            component[start] = label;
            stack.push(start);

            while (!stack.isEmpty()) {
                int v = stack.pop();

                for (int to : reverse.get(v)) {
                    if (component[to] == -1) {
                        component[to] = label;
                        stack.push(to);
                    }
                }
            }

            label++;
        }

        Set<String> seen = new HashSet<>();
        List<int[]> condensation = new ArrayList<>();

        for (int[] edge : edges) {
            int from = component[edge[0]];
            int to = component[edge[1]];
            String pair = from + "," + to;

            if (from != to && seen.add(pair)) {
                condensation.add(new int[]{from, to});
            }
        }

        return Map.of("component", component, "condensation", condensation);
    }

    static Map<String, Object> topo(Map<String, Object> input) {
        int n = Input.vertexCount(input);
        int[][] edges = Input.edges(input);
        DfsResult result = dfs(directedGraph(n, edges));

        if (!result.acyclic) {
            return Map.of("acyclic", false, "order", List.of());
        }

        return Map.of("acyclic", true, "order", result.order);
    }

    static Map<String, Object> dag(Map<String, Object> input) {
        int n = Input.vertexCount(input);
        int source = Input.source(input);
        int[][] edges = Input.edges(input);
        DfsResult result = dfs(directedGraph(n, edges));

        if (!result.acyclic) {
            return Map.of("acyclic", false, "distance", List.of(), "parent_edge", List.of());
        }

        List<List<Integer>> outgoing = emptyGraph(n);

        for (int id = 0; id < edges.length; id++) {
            outgoing.get(edges[id][0]).add(id);
        }

        Long[] distance = new Long[n];
        int[] parentEdge = new int[n];
        Arrays.fill(parentEdge, -1);
        distance[source] = 0L;

        for (int v : result.order) {
            if (distance[v] == null) {
                continue;
            }

            for (int id : outgoing.get(v)) {
                int to = edges[id][1];
                long candidate = distance[v] + edges[id][2];

                if (distance[to] == null || candidate < distance[to]) {
                    distance[to] = candidate;
                    parentEdge[to] = id;
                }
            }
        }

        return Map.of("acyclic", true, "distance", distance, "parent_edge", parentEdge);
    }

    static List<Integer> reconstructRoute(int source, int target, int[][] edges, int[] parentEdge) {
        if (source < 0 || source >= parentEdge.length || target < 0 || target >= parentEdge.length) {
            throw new IllegalArgumentException("Invalid source or target");
        }

        List<Integer> route = new ArrayList<>();

        if (target != source && parentEdge[target] == -1) {
            return route;
        }

        int current = target;

        while (current != source) {
            if (route.size() >= parentEdge.length) {
                throw new IllegalArgumentException("Parent cycle");
            }

            route.add(current);
            int id = parentEdge[current];

            if (id < 0 || id >= edges.length || edges[id][1] != current) {
                throw new IllegalArgumentException("Invalid parent edge");
            }

            current = edges[id][0];

            if (current < 0 || current >= parentEdge.length) {
                throw new IllegalArgumentException("Invalid parent vertex");
            }
        }

        route.add(source);
        Collections.reverse(route);
        return route;
    }
}

