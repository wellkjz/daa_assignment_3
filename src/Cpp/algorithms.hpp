#pragma once
#include "input.hpp"

inline J dsu(const J& input) {
    int n = Input::vertexCount(input);
    vector<vector<int>> operations = Input::operations(input);

    throw runtime_error("TODO dsu");
}

inline J kruskal(const J& input) {
    int n = Input::vertexCount(input);
    vector<vector<int>> edges = Input::edges(input);

    throw runtime_error("TODO kruskal");
}

inline J prim(const J& input) {
    int n = Input::vertexCount(input);
    vector<vector<int>> edges = Input::edges(input);

    throw runtime_error("TODO prim");
}

inline J scc(const J& input) {
    int n = Input::vertexCount(input);
    vector<vector<int>> edges = Input::edges(input);

    throw runtime_error("TODO scc");
}

inline J topo(const J& input) {
    int n = Input::vertexCount(input);
    vector<vector<int>> edges = Input::edges(input);

    throw runtime_error("TODO topo");
}

inline J dag(const J& input) {
    int n = Input::vertexCount(input);
    int source = Input::source(input);
    vector<vector<int>> edges = Input::edges(input);

    throw runtime_error("TODO dag");
}

