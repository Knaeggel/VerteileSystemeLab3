public class InfoLogMessage {
    String type = "infoLog";

    String timestamp;

    String startIp;

    String startPort;

    String destinationIp;

    String destinationPort;

    /**
     * Can be info or echo
     */
    String messageType;

    int partialSum;


    public InfoLogMessage() {
    }

    public InfoLogMessage(String timestamp,
                          String startIp,
                          String startPort,
                          String destinationIp,
                          String destinationPort,
                          String messageType,
                          int partialSum) {
        this.timestamp = timestamp;
        this.startIp = startIp;
        this.startPort = startPort;
        this.destinationIp = destinationIp;
        this.destinationPort = destinationPort;
        this.messageType = messageType;
        this.partialSum = partialSum;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getStartIp() {
        return startIp;
    }

    public void setStartIp(String startIp) {
        this.startIp = startIp;
    }

    public String getStartPort() {
        return startPort;
    }

    public void setStartPort(String startPort) {
        this.startPort = startPort;
    }

    public String getDestinationIp() {
        return destinationIp;
    }

    public void setDestinationIp(String destinationIp) {
        this.destinationIp = destinationIp;
    }

    public String getDestinationPort() {
        return destinationPort;
    }

    public void setDestinationPort(String destinationPort) {
        this.destinationPort = destinationPort;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public int getPartialSum() {
        return partialSum;
    }

    public void setPartialSum(int partialSum) {
        this.partialSum = partialSum;
    }
}
