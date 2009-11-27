package de.asv.graph;


public class Edge extends GraphItem{
	private Node source;
	private Node target;
	
	protected Edge(int id, Node sourceNode, Node targetNode) {
		super(id);
		source = sourceNode;
		target = targetNode;
	}
	
	public Node getSourceNode() {
		return source;
	}
	
	public Node getTargetNode() {
		return target;
	}
}
