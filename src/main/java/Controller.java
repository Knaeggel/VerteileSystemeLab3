import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;

public class Controller {

    public static void main(String[] args) {

        new Controller().start();
    }

    public void start() {
        try {
            ObjectMapper objectMapper = new ObjectMapper();

            Path path = Paths.get("src/main/java/mynet.json");

            BufferedReader reader = Files.newBufferedReader(path);

            StringBuilder stringBuilder = new StringBuilder();
            String line = reader.readLine();
            while (line != null) {
                line = line.trim().replaceAll("\\s+", " ");
                stringBuilder.append(line);
                line = reader.readLine();
            }

            String nodeNetworkAsString = stringBuilder.toString();

            NodeNetwork nodeNetwork = objectMapper.readValue(nodeNetworkAsString, NodeNetwork.class);

            List<Node> nodes = nodeNetwork.getNodes();

            // Init sockets
            HashMap<Integer, DatagramSocket> sockets = new HashMap<Integer, DatagramSocket>();

            nodes.forEach(node -> {
                try {
                    sockets.put(node.getPort(), new DatagramSocket(node.getPort()));
                } catch (SocketException e) {
                    throw new RuntimeException(e);
                }
            });



            startNodes(nodes, sockets);

            startController(nodes, sockets, objectMapper);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startNodes(List<Node> nodes, HashMap<Integer, DatagramSocket> sockets) {


        for (Node node : nodes) {

            new Thread(() -> {
                boolean initiator = false;

                int ownPort = node.getPort();

                DatagramSocket socket = sockets.get(ownPort);

                // Init receive buffer
                byte[] receiveBuffer = new byte[1024];

                DatagramPacket datagramPacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                while (true) {
                    try {
                        socket.receive(datagramPacket);

                        String data = new String(datagramPacket.getData(), 0, datagramPacket.getLength());

                        if (data.contains("start")) {
                            System.out.println("start was called");
                        }
                        else {
                            System.out.println("Start was not called");
                        }


                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }).start();
        }
    }

    public void startController(List<Node> nodes, HashMap<Integer, DatagramSocket> sockets, ObjectMapper objectMapper) throws IOException {

        DatagramSocket initiatorSocket = sockets.get(50001);

        Node initiatorNode = nodes.get(0);

        InetAddress initiatorIp = InetAddress.getByName(initiatorNode.getIp());

        byte[] message = objectMapper.writeValueAsString(new StartMessage()).getBytes();

        DatagramPacket startDatagramPacket = new DatagramPacket(message, message.length, initiatorIp, initiatorNode.getPort());

        initiatorSocket.send(startDatagramPacket);

        while (true) {

        }

    }
}
