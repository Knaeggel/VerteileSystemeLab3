import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import messages.*;

public class Controller {


    /**
     * Starts the controller
     * @param nodes List of all the nodes in the network
     * @param controllerSocket The socket of the controller
     * @param nodeNetwork The node network
     * @throws IOException
     * @throws InterruptedException
     */
    public void startController(List<Node> nodes, DatagramSocket controllerSocket, NodeNetwork nodeNetwork) throws IOException, InterruptedException {

        Scanner scanner = new Scanner(System.in);

        ObjectMapper objectMapper = new ObjectMapper();

        System.out.println("Available initiators: " + getAvailableInitiators(nodeNetwork));

        System.out.println("Total nodes: " + nodeNetwork.getNodes().size());

        System.out.println("Total edges: " + calculateEdges(nodeNetwork));

        System.out.println("Ip and port of initiator: (IP-Address:Port)");

        String initiatorAddress = scanner.next();

        // Extract ip and port
        String[] ipAndPort = initiatorAddress.split(":");

        String initiatorIpString = ipAndPort[0];

        if (initiatorIpString.equals("localhost")) {
            initiatorIpString = "127.0.0.1";
        }

        int initiatorPort = Integer.parseInt(ipAndPort[1]);

        Node initiatorNode = null;

        // Determine initiator node
        for (Node node : nodes) {
            if (node.getIp().equals(initiatorIpString) && node.getPort() == initiatorPort) {
                initiatorNode = node;
            }
        }

        // If initiator node could not be found
        if (initiatorNode == null) {
            System.out.println("Cound not find initiator node");

            return;
        }

        InetAddress initiatorIp = InetAddress.getByName(initiatorNode.getIp());

        byte[] message = objectMapper.writeValueAsString(new StartMessage()).getBytes();

        // Create and send start message to initiator
        DatagramPacket startDatagramPacket = new DatagramPacket(message, message.length, initiatorIp, initiatorNode.getPort());
        controllerSocket.send(startDatagramPacket);

        byte[] receiveBuffer = new byte[1024];

        // Create recieve datagram socket
        DatagramPacket datagramPacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);

        int infoMessageCount = 0;
        int echoMessageCount = 0;

        boolean controllerRunning = true;

        // While controller is runnin
        while (controllerRunning) {

            // Recieve logs and result
            controllerSocket.receive(datagramPacket);

            String data = new String(datagramPacket.getData(), 0, datagramPacket.getLength());

            if (data.contains("result")) {
                ResultMessage resultMessage = objectMapper.readValue(data, ResultMessage.class);

                System.out.println("Total sum: " + resultMessage.getSum());
                System.out.println("Total info messages: " + infoMessageCount);
                System.out.println("Total echo messages: " + echoMessageCount);

                System.out.println("Stopped controller");

                controllerRunning = false;

            } else if (data.contains("infoLog")) {
                InfoLogMessage infoLogMessage = objectMapper.readValue(data, InfoLogMessage.class);

                if (data.contains("info")) {
                    infoMessageCount++;
                }
                if (data.contains("echo")) {
                    echoMessageCount++;
                }

                infoLogMessage.printInfoLogMessage();
            }
        }
    }

    /**
     * Calculates all the edges in the given nodeNetwork
     * @param nodeNetwork
     * @return number of edges
     */
    public int calculateEdges(NodeNetwork nodeNetwork) {
        int edgesCount = 0;

        List<ReducedNode> nodesToIgnore = new ArrayList<>();

        for (Node node : nodeNetwork.getNodes()) {

            String myIp = node.getIp();
            int myPort = node.getPort();

            List<ReducedNode> neighbors = node.getNeighbors();

            for (ReducedNode neighbor : neighbors) {
                if (!nodesToIgnore.contains(neighbor)) {
                    edgesCount++;

                    nodesToIgnore.add(new ReducedNode(myIp, myPort));
                }
            }
        }

        return edgesCount;
    }

    /**
     * Gets all the available initiators in the given nodeNetwork
     * @param nodeNetwork
     * @return The available initiators
     */
    public String getAvailableInitiators(NodeNetwork nodeNetwork) {
        StringBuilder availableInitiators = new StringBuilder();

        for (Node node : nodeNetwork.getNodes()) {
            availableInitiators.append(node.getIp() + ":" + node.getPort() + " ");
        }

        return availableInitiators.toString();
    }

}
