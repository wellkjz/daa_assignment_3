using System.Text.Json.Nodes;

public static class Algorithms
{

    public static JsonObject dsu(JsonObject input)
    {
        int n = Input.VertexCount(input);
        int[][] operations = Input.Operations(input);

        throw new NotImplementedException("TODO dsu");
    }

    public static JsonObject kruskal(JsonObject input)
    {
        int n = Input.VertexCount(input);
        int[][] edges = Input.Edges(input);

        throw new NotImplementedException("TODO kruskal");
    }

    public static JsonObject prim(JsonObject input)
    {
        int n = Input.VertexCount(input);
        int[][] edges = Input.Edges(input);

        throw new NotImplementedException("TODO prim");
    }

    public static JsonObject scc(JsonObject input)
    {
        int n = Input.VertexCount(input);
        int[][] edges = Input.Edges(input);

        throw new NotImplementedException("TODO scc");
    }

    public static JsonObject topo(JsonObject input)
    {
        int n = Input.VertexCount(input);
        int[][] edges = Input.Edges(input);

        throw new NotImplementedException("TODO topo");
    }

    public static JsonObject dag(JsonObject input)
    {
        int n = Input.VertexCount(input);
        int source = Input.Source(input);
        int[][] edges = Input.Edges(input);

        throw new NotImplementedException("TODO dag");
    }
}

