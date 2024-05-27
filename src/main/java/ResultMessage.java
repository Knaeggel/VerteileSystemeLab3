public class ResultMessage {
    String type = "result";
    int sum;

    public ResultMessage() {

    }

    public ResultMessage(int sum) {
        this.sum = sum;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getSum() {
        return sum;
    }

    public void setSum(int sum) {
        this.sum = sum;
    }
}
