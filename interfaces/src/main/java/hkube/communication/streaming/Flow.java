package hkube.communication.streaming;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Flow {

    List<Node> nodes = new ArrayList<>();

    public List<Node> getNodes() {
        return nodes;
    }

    public void setNodes(List<Node> nodes) {
        this.nodes = nodes;
    }

    public Flow(List mapList) {

        ((List<Map>) mapList).stream().forEach(
                node -> {
                    nodes.add(new Node((String) node.get("source"), (List) node.get("next")));
                }
        );
    }

    public class Node {
        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public List<String> getNext() {
            return next;
        }

        public void setNext(List<String> next) {
            this.next = next;
        }

        public Node(String source, List<String> next) {
            this.source = source;
            this.next = next;
        }

        String source;
        List<String> next;
    }


    public boolean isNextInFlow(String current, String next) {
        Node currentNode = getNode(current);
        if (currentNode != null) {
            return currentNode.next.contains(next);
        } else {
            return false;
        }
    }

    private Node getNode(String name) {
        if(nodes.stream().anyMatch(node ->
                (node.source.equals(name))))
        return nodes.stream().filter(node ->
                (node.source.equals(name))
        ).findFirst().get();
        else
            return null;
    }

    public Flow getRestOfFlow(String current) {
        Flow flow = clone();
        flow.nodes.remove(getNode(current));
        return flow;
    }

    public Flow clone() {
        Flow flow = new Flow(new ArrayList<>());
        List<Node> nodesCopy = new ArrayList<>();
        nodesCopy.addAll(nodes);
        flow.setNodes(nodesCopy);
        return flow;
    }
    public List asList(){
        return nodes;
    }
}
