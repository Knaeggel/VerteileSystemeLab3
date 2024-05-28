public class EchoMessage {
    String type = "echo";

    int partialSum;

    public EchoMessage(int partialSum) {
        this.partialSum = partialSum;
    }

    public EchoMessage() {

    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getPartialSum() {
        return partialSum;
    }

    public void setPartialSum(int partialSum) {
        this.partialSum = partialSum;
    }

    @Override
    public String toString() {
        return "EchoMessage [type=" + type + ", partialSum=" + partialSum + "]";
    }
}
