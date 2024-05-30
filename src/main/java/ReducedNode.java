public class ReducedNode {
    String ip;

    int port;


    public ReducedNode(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public ReducedNode() {

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

    @Override
    public boolean equals(Object obj) {
        return ip.equals(((ReducedNode) obj).ip) && port == ((ReducedNode) obj).port;
    }
}
