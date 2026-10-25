import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class RouteTests {
    static void require(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }

    static void test(Map<String,Object> request, Map<String,Object> result, int target,
                     List<Integer> expected) {
        int source = Input.source(request);
        int[][] edges = Input.edges(request);
        int[] parents = (int[]) result.get("parent_edge");
        Long[] distance = (Long[]) result.get("distance");
        List<Integer> route = Algorithms.reconstructRoute(source, target, edges, parents);
        require(route.equals(expected), "Unexpected route to " + target);
        List<Integer> edgeIds = new ArrayList<>();
        long cost = 0;
        for (int i = 1; i < route.size(); i++) {
            int id = parents[route.get(i)];
            require(edges[id][0] == route.get(i-1) && edges[id][1] == route.get(i), "Wrong direction");
            edgeIds.add(id);
            cost += (long) edges[id][2];
        }
        require(route.isEmpty() ? distance[target] == null : cost == distance[target], "Wrong cost");
        System.out.println(Json.write(Map.of("source",source,"target",target,
                "route",route,"edge_ids",edgeIds,"cost",route.isEmpty() ? "no route" : cost)));
    }

    public static void main(String[] args) {
        Map<String,Object> x = Json.parseRequest(
            "{\"task\":\"dag\",\"n\":5,\"source\":0,\"edges\":[[0,1,3],[0,2,8],[1,2,-7],[2,3,4]]}");
        Map<String,Object> y = Algorithms.dag(x);
        test(x,y,3,List.of(0,1,2,3));
        test(x,y,0,List.of(0));
        test(x,y,4,List.of());
        Map<String,Object> tied = Json.parseRequest(
            "{\"task\":\"dag\",\"n\":4,\"source\":0,\"edges\":[[0,1,8],[0,1,2],[1,2,-1],[0,2,1]]}");
        Map<String,Object> answer = Algorithms.dag(tied);
        test(tied,answer,1,List.of(0,1));
        test(tied,answer,2,List.of(0,2));
        int[] alternative = {-1,1,2,-1};
        require(Algorithms.reconstructRoute(0,2,Input.edges(tied),alternative).equals(List.of(0,1,2)),
                "A different valid tied witness must reconstruct too");
        boolean rejected = false;
        try {
            Algorithms.reconstructRoute(0,1,new int[][]{{2,1,0},{1,2,0}},new int[]{-1,0,1});
        } catch (IllegalArgumentException error) {
            rejected = true;
        }
        require(rejected,"Malformed parent cycle must terminate");
    }
}
