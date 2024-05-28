import java.util.List;

public class Node {
    String ip;
    int port;

    /**
     * (memory)
     */
    int nodeId;

    List<ReducedNode> neighbors;

    public Node() {
    }

    public Node(String ip, int port, int nodeId, List<ReducedNode> neighbors) {
        this.ip = ip;
        this.port = port;
        this.nodeId = nodeId;
        this.neighbors = neighbors;
    }

    public Node(String ip, int port, int nodeId) {
        this.ip = ip;
        this.port = port;
        this.nodeId = nodeId;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getNodeId() {
        return nodeId;
    }

    public void setNodeId(int nodeId) {
        this.nodeId = nodeId;
    }

    public List<ReducedNode> getNeighbors() {
        return neighbors;
    }

    public void setNeighbors(List<ReducedNode> neighbors) {
        this.neighbors = neighbors;
    }

    @Override
    public String toString() {
        return ip + ":" + port;
    }
}
