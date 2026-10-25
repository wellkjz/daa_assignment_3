import java.util.*;

public class Benchmark {
    private static final int WARMUP = 5;
    private static final int REPEATS = 5;
    private static volatile long sink;

    private static int[][] makeGraph(int n, int m, long seed) {
        Random random = new Random(seed);
        int[][] edges = new int[m][3];

        for (int i = 0; i < n - 1; i++) {
            edges[i][0] = i;
            edges[i][1] = i + 1;
            edges[i][2] = -1 - random.nextInt(1_000_000);
        }

        for (int i = n - 1; i < m; i++) {
            edges[i][0] = random.nextInt(n);
            edges[i][1] = random.nextInt(n);
            edges[i][2] = random.nextInt(1_000_001);
        }

        return edges;
    }

    private static void validate(int n, int[][] edges, Map<String, Object> result) {
        List<?> ids = (List<?>) result.get("edge_ids");
        boolean[] seen = new boolean[n - 1];
        long cost = 0;

        if (ids.size() != n - 1 || ((Number) result.get("components")).intValue() != 1) {
            throw new AssertionError("Wrong tree size");
        }

        for (Object value : ids) {
            int id = ((Number) value).intValue();

            if (id < 0 || id >= n - 1 || seen[id]) {
                throw new AssertionError("The selected edges are not the known minimum tree");
            }

            seen[id] = true;
            cost += edges[id][2];
        }

        if (((Number) result.get("cost")).longValue() != cost) {
            throw new AssertionError("Wrong tree cost");
        }
    }

    private static Map<String, Object> run(String method, int n, int[][] edges) {
        if (method.equals("kruskal")) {
            return Algorithms.kruskal(n, edges);
        }
        return Algorithms.prim(n, edges);
    }

    private static double measure(String method, int n, int[][] edges, long expectedCost) {
        long start = System.nanoTime();
        Map<String, Object> result = run(method, n, edges);
        long elapsed = System.nanoTime() - start;

        sink = ((Number) result.get("cost")).longValue();
        if (sink != expectedCost) {
            throw new AssertionError("Wrong measured result");
        }

        return elapsed / 1_000_000.0;
    }

    private static double median(double[] times) {
        double[] sorted = times.clone();
        Arrays.sort(sorted);
        return sorted[sorted.length / 2];
    }

    public static void main(String[] args) {
        System.out.println(Json.write(Map.of(
                "java", System.getProperty("java.runtime.version"),
                "vm", System.getProperty("java.vm.name"),
                "os", System.getProperty("os.name") + " " + System.getProperty("os.version"),
                "logical_processors", Runtime.getRuntime().availableProcessors(),
                "max_heap_bytes", Runtime.getRuntime().maxMemory(),
                "timer", "System.nanoTime",
                "warmup_per_method_per_size", WARMUP,
                "repetitions_per_method", REPEATS)));

        for (int n : new int[]{1000, 10000, 100000}) {
            int m = n * 5 / 2;
            long seed = 20261025L + n;
            int[][] edges = makeGraph(n, m, seed);

            Map<String, Object> kruskalResult = Algorithms.kruskal(n, edges);
            Map<String, Object> primResult = Algorithms.prim(n, edges);
            validate(n, edges, kruskalResult);
            validate(n, edges, primResult);
            long expectedCost = ((Number) kruskalResult.get("cost")).longValue();

            for (int i = 0; i < WARMUP; i++) {
                measure("kruskal", n, edges, expectedCost);
                measure("prim", n, edges, expectedCost);
            }

            double[] kruskalTimes = new double[REPEATS];
            double[] primTimes = new double[REPEATS];

            for (int i = 0; i < REPEATS; i++) {
                if (i % 2 == 0) {
                    kruskalTimes[i] = measure("kruskal", n, edges, expectedCost);
                    primTimes[i] = measure("prim", n, edges, expectedCost);
                } else {
                    primTimes[i] = measure("prim", n, edges, expectedCost);
                    kruskalTimes[i] = measure("kruskal", n, edges, expectedCost);
                }
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("n", n);
            result.put("m", m);
            result.put("seed", seed);
            result.put("cost", expectedCost);
            result.put("components", 1);
            result.put("validation", "PASS: both results equal the known minimum tree");
            result.put("kruskal_ms", kruskalTimes);
            result.put("prim_ms", primTimes);
            result.put("kruskal_median_ms", median(kruskalTimes));
            result.put("prim_median_ms", median(primTimes));
            System.out.println(Json.write(result));
        }
    }
}
