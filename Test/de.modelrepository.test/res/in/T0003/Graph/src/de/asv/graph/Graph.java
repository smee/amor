package de.asv.graph;

import java.util.HashSet;
import java.util.Iterator;

public class Graph {
	private HashSet<Node> nodes = new HashSet<Node>();
	private HashSet<Edge> edges = new HashSet<Edge>();
	private String id;
	
	public Graph(String id) {
		this.id = id;
	}
	
	public int nodeCount() {
		return nodes.size();
	}
	
	public int edgeCount() {
		return edges.size();
	}
	
	public Iterator<Node> getNodes() {
		return nodes.iterator();
	}
	
	public Iterator<Edge> getEdges() {
		return edges.iterator();
	}
	
	public Node getNode(int id) {
		for (Node node : nodes) {
			if(node.getId() == id)
				return node;
		}
		return null;
	}
	
	public Edge getEdge(int sourceNode, int targetNode) {
		for (Edge edge : edges) {
			if(edge.getId() == createEdgeId(sourceNode, targetNode))
				return edge;
		}
		return null;
	}
	
	//TODO The id must be calculated in a different way. Redundant ids may occur. 
	private int createEdgeId(int source, int target) {
		return source*target;
	}
	
	public Node createNode(int id) {
		Node n = getNode(id);
		if(n == null)
			n = new Node(id);
		nodes.add(n);
		return n;
	}
	
	public Edge createEdge(int sourceNodeId, int targetNodeId) {
		Edge e = getEdge(sourceNodeId, targetNodeId);
		if(e == null) {
			Node source = getNode(sourceNodeId);
			Node target = getNode(targetNodeId);
			int id = createEdgeId(sourceNodeId, targetNodeId);
			e = new Edge(id, source, target);
		}
		edges.add(e);
		return e;
	}
	
	public String getId() {
		return id;
	}
}
