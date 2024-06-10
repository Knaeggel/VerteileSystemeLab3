import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import messages.*;

public class NodeApplication {

    public static String controllerIp = "192.168.3.2";
    public static int controllerPort = 50000;

    // args should look like this localhost 50001 localhost:50002;localhost:50003 1
    public static void main(String[] args) throws IOException {

        ObjectMapper objectMapper = new ObjectMapper();

        if (args.length < 4) {
            System.err.println("Insufficient arguments. Required: <initialIp> <initialPort> <initialNeighbors> <initialMemory>");
            System.exit(1);
        }

        String initialIp = args[0];

        int initialPort = Integer.parseInt(args[1]);

        List<ReducedNode> initialNeighbors = extractIpAndPort(args[2]);

        int initialMemory = Integer.parseInt(args[3]);

        Node node = new Node(initialIp, initialPort, initialMemory, initialNeighbors);


        InetAddress controllerAdress = InetAddress.getByName(controllerIp);


        try {

            boolean initiator = false;
            boolean informed = false;
            int neighborsInformed = 0;

            int ownSum = node.getNodeId();

            String ownIp = node.getIp();

            int ownPort = node.getPort();

            DatagramSocket socket = new DatagramSocket(node.getPort());

            List<ReducedNode> neighbors = node.getNeighbors();

            ReducedNode parentNode = null;

            // Init receive buffer
            byte[] receiveBuffer = new byte[1024];

            DatagramPacket receiveDatagramPacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);

            while (true) {

                socket.receive(receiveDatagramPacket);

                // Add latency
                Thread.sleep((int) (Math.random() * 100));

                String data = new String(receiveDatagramPacket.getData(), 0, receiveDatagramPacket.getLength());

                // If it is the start message
                if (data.contains("start")) {
                    System.out.println("start was called on: " + ownIp + ":" + ownPort);

                    initiator = true;
                    informed = true;

                    InfoMessage infoMessage = new InfoMessage(ownIp, ownPort);

                    String infoData = objectMapper.writeValueAsString(infoMessage);

                    // Send info to all the neighbors
                    sendInfoMessageToNeighborsAndController(socket, neighbors, infoData, ownIp, ownPort, controllerAdress, controllerPort, objectMapper);

                } else if (data.contains("info")) {

                    neighborsInformed++;

                    if (informed == false) {

                        informed = true;

                        InfoMessage infoMessage = objectMapper.readValue(data, InfoMessage.class);

                        // Read parent node from the info message
                        parentNode = new ReducedNode(infoMessage.getSenderIp(), infoMessage.getSenderPort());

                        List<ReducedNode> nodesWithoutParent = new ArrayList<>();

                        nodesWithoutParent.addAll(neighbors);
                        nodesWithoutParent.remove(parentNode);

                        InfoMessage ownInfoMessage = new InfoMessage(ownIp, ownPort);

                        String newInfoData = objectMapper.writeValueAsString(ownInfoMessage);

                        // Send info message to all the neighbors but not the parent
                        sendInfoMessageToNeighborsAndController(socket, nodesWithoutParent, newInfoData, ownIp, ownPort, controllerAdress, controllerPort, objectMapper);
                    }

                    if (neighborsInformed == neighbors.size()) {

                        // Send the initial echo
                        EchoMessage echoMessage = new EchoMessage(ownSum);

                        String echoData = objectMapper.writeValueAsString(echoMessage);

                        sendEchoMessageToParentAndController(socket, parentNode, echoData, ownIp, ownPort, controllerAdress, controllerPort, objectMapper, ownSum);
                    }


                } else if (data.contains("echo")) {

                    neighborsInformed++;

                    EchoMessage echoMessage = objectMapper.readValue(data, EchoMessage.class);

                    // Add echo sum to own sum
                    ownSum += echoMessage.getPartialSum();

                    echoMessage.setPartialSum(ownSum);

                    String echoData = objectMapper.writeValueAsString(echoMessage);

                    if (neighborsInformed == neighbors.size()) {

                        // If the initiator recieved echos from all its children
                        if (initiator) {

                            ResultMessage resultMessage = new ResultMessage(ownSum);

                            byte[] resultData = objectMapper.writeValueAsBytes(resultMessage);

                            DatagramPacket resultDatagramPacket = new DatagramPacket(resultData, resultData.length, controllerAdress, controllerPort);

                            System.out.println("Echo terminated");

                            socket.send(resultDatagramPacket);

                        } else {

                            // Send echo to parent
                            sendEchoMessageToParentAndController(socket, parentNode, echoData, ownIp, ownPort, controllerAdress, controllerPort, objectMapper, ownSum);
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.exit(0);
        }

    }

    /**
     * Sends an info message to all the given neighbors
     * @param socket The socket of the node
     * @param neighbors The list of neighbors to send to
     * @param data The data to send
     * @param ownIp
     * @param ownPort
     * @param controllerAdress
     * @param controllerPort
     * @param objectMapper
     * @throws IOException
     * @throws InterruptedException
     */
    public static void sendInfoMessageToNeighborsAndController(DatagramSocket socket, List<ReducedNode> neighbors, String data, String ownIp, int ownPort, InetAddress controllerAdress, int controllerPort, ObjectMapper objectMapper) throws IOException, InterruptedException {

        // For each neighbor
        for (ReducedNode neighbor : neighbors) {

            // Get ip
            String neighborIp = neighbor.getIp();

            // Get port
            int neighborPort = neighbor.getPort();

            // Get address
            InetAddress neighborAddress = InetAddress.getByName(neighborIp);

            // Create info packet
            DatagramPacket infoPacket = new DatagramPacket(data.getBytes(), data.length(), neighborAddress, neighborPort);

            // Send info packet
            socket.send(infoPacket);

            // Create info log message
            InfoLogMessage infoLogMessage = new InfoLogMessage(new Date().toString(), ownIp, ownPort, neighborIp, neighborPort, "info", 0);

            // Convert log info message to bytes
            byte[] infoLogMessageAsByte = objectMapper.writeValueAsBytes(infoLogMessage);

            // create controller datagram packet
            DatagramPacket controllerDatagramPacket = new DatagramPacket(infoLogMessageAsByte, infoLogMessageAsByte.length, controllerAdress, controllerPort);

            socket.send(controllerDatagramPacket);
        }
    }

    /**
     * Sends the echo message to the parent and the controller
     * @param socket The socket of the node
     * @param parent The parent node
     * @param data The recieved echo message data
     * @param ownIp
     * @param ownPort
     * @param controllerAddress
     * @param controllerPort
     * @param objectMapper
     * @param partialSum The current partial sum
     * @throws IOException
     * @throws InterruptedException
     */
    public static void sendEchoMessageToParentAndController(DatagramSocket socket, ReducedNode parent, String data, String ownIp, int ownPort, InetAddress controllerAddress, int controllerPort, ObjectMapper objectMapper, int partialSum) throws IOException, InterruptedException {

        // Get ip
        String parentIp = parent.getIp();

        // Get port
        int parentPort = parent.getPort();

        // Get address
        InetAddress neighborAddress = InetAddress.getByName(parentIp);

        // Create info packet
        DatagramPacket infoPacket = new DatagramPacket(data.getBytes(), data.length(), neighborAddress, parentPort);

        // Send info packet
        socket.send(infoPacket);

        // Create info log message
        InfoLogMessage infoLogMessage = new InfoLogMessage(new Date().toString(), ownIp, ownPort, parentIp, parentPort, "echo", partialSum);

        // Convert log info message to bytes
        byte[] infoLogMessageAsByte = objectMapper.writeValueAsBytes(infoLogMessage);

        // create controller datagram packet
        DatagramPacket controllerDatagramPacket = new DatagramPacket(infoLogMessageAsByte, infoLogMessageAsByte.length, controllerAddress, controllerPort);

        socket.send(controllerDatagramPacket);

    }

    /**
     * Extracts the ips and ports from the given String argument
     * @param stringArgument The string argument
     * @return List of parsed nodes from the argument
     */
    public static List<ReducedNode> extractIpAndPort(String stringArgument) {
        List<ReducedNode> nodes = new ArrayList<>();

        String[] ipAndPorts = stringArgument.split(";");

        for (String ipAndPort : ipAndPorts) {
            String[] ipAndPortParts = ipAndPort.split(":");

            String ip = ipAndPortParts[0];
            int port = Integer.parseInt(ipAndPortParts[1]);

            nodes.add(new ReducedNode(ip, port));
        }

        return nodes;
    }

}
