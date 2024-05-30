package messages;

public class StartMessage {
    String type;

    public StartMessage(String type) {
        this.type = type;
    }

    public StartMessage() {
        this.type = "start";
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
