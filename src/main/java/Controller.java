import com.fasterxml.jackson.databind.ObjectMapper;

import javax.sound.midi.Soundbank;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Controller {

    public static String controllerIp = "127.0.0.1";
    public static int controllerPort = 50000;

    public static void main(String[] args) {

        new Controller().start();
    }

    public void start() {

        List<Process> nodeProcesses = null;

        try {
            // Create object mapper
            ObjectMapper objectMapper = new ObjectMapper();

            // Path to my network
            Path path = Paths.get("src/main/java/mynet.json");

            // Create bufferd reader
            BufferedReader reader = Files.newBufferedReader(path);

            // Build mynet.json as string
            StringBuilder stringBuilder = new StringBuilder();

            String line = reader.readLine();
            while (line != null) {
                line = line.trim().replaceAll("\\s+", " ");
                stringBuilder.append(line);
                line = reader.readLine();
            }

            String nodeNetworkAsString = stringBuilder.toString();

            // Create node network from json file
            NodeNetwork nodeNetwork = objectMapper.readValue(nodeNetworkAsString, NodeNetwork.class);

            List<Node> nodes = nodeNetwork.getNodes();

            DatagramSocket controllerSocket = new DatagramSocket(controllerPort);

            nodeProcesses = startNodes(nodes);

            startController(nodes, objectMapper, controllerSocket, nodeNetwork);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {

            System.out.println("Destroying node processes");
            if (nodeProcesses != null) {
                for (Process nodeProcess : nodeProcesses) {
                    try {
                        if (nodeProcess.isAlive()) {
                            nodeProcess.destroy();
                            if (!nodeProcess.waitFor(1, TimeUnit.SECONDS)) {

                                nodeProcess.destroyForcibly();
                            }
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public List<Process> startNodes(List<Node> nodes) {

        List<Process> processes = new ArrayList<>();

        String currentDir = System.getProperty("user.dir");

        String jarPath = currentDir + File.separator + "out" + File.separator + "artifacts" + File.separator + "VerteileSystemeLab3Node_jar" + File.separator + "VerteileSystemeLab3.jar";
        jarPath = currentDir + File.separator + "VerteileSystemeLab3.jar";


        for (Node node : nodes) {

            String myIp = node.getIp();

            int myPort = node.getPort();

            String ipsAndPortsArgs = parseIpAndPortsToStringArg(node.getNeighbors());

            int memory = node.getNodeId();

            ProcessBuilder processBuilder = new ProcessBuilder("java", "-jar", jarPath, myIp, String.valueOf(myPort), ipsAndPortsArgs, String.valueOf(memory));

            try {
                Process process = processBuilder.start();
                processes.add(process);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return processes;
    }

    public String parseIpAndPortsToStringArg(List<ReducedNode> neighbors) {

        StringBuilder ipsAndPortsArgs = new StringBuilder();

        for (ReducedNode neighbor : neighbors) {

            ipsAndPortsArgs.append(neighbor.getIp() + ":" + neighbor.getPort() + ";");
        }

        return ipsAndPortsArgs.toString();
    }


    public void startController(List<Node> nodes, ObjectMapper objectMapper, DatagramSocket controllerSocket, NodeNetwork nodeNetwork) throws IOException {

        Scanner scanner = new Scanner(System.in);

        System.out.println("Total nodes: " + nodeNetwork.getNodes().size());

        System.out.println("Total edges: " + calculateEdges(nodeNetwork));

        System.out.println("ip and port of initiator: ");

        String initiatorIpString = scanner.next();

        if (initiatorIpString.equals("localhost")) {
            initiatorIpString = "127.0.0.1";
        }

        int initiatorPort = Integer.parseInt(scanner.next());

        Node initiatorNode = null;

        for (Node node : nodes) {
            if (node.getIp().equals(initiatorIpString) && node.getPort() == initiatorPort) {
                initiatorNode = node;
            }
        }

        InetAddress initiatorIp = InetAddress.getByName(initiatorNode.getIp());

        byte[] message = objectMapper.writeValueAsString(new StartMessage()).getBytes();

        DatagramPacket startDatagramPacket = new DatagramPacket(message, message.length, initiatorIp, initiatorNode.getPort());

        controllerSocket.send(startDatagramPacket);

        byte[] receiveBuffer = new byte[1024];

        DatagramPacket datagramPacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);

        int infoMessageCount = 0;
        int echoMessageCount = 0;

        boolean controllerRunning = true;

        while (controllerRunning) {

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


    /*
    public void startNodes(List<Node> nodes) throws IOException {

        InetAddress controllerAdress = InetAddress.getByName(controllerIp);

        ObjectMapper objectMapper = new ObjectMapper();

        for (Node node : nodes) {

            new Thread(() -> {
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

                        String data = new String(receiveDatagramPacket.getData(), 0, receiveDatagramPacket.getLength());

                        if (data.contains("start")) {
                            System.out.println("start was called on: " + ownIp + ":" + ownPort);

                            initiator = true;
                            informed = true;

                            InfoMessage infoMessage = new InfoMessage(ownIp, ownPort);

                            String infoData = objectMapper.writeValueAsString(infoMessage);

                            sendInfoMessageToNeighborsAndController(socket, neighbors, infoData, ownIp, ownPort, controllerAdress, objectMapper);

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

                                sendInfoMessageToNeighborsAndController(socket, nodesWithoutParent, newInfoData, ownIp, ownPort, controllerAdress, objectMapper);
                            }

                            if (neighborsInformed == neighbors.size()) {

                                // Send echo
                                EchoMessage echoMessage = new EchoMessage(ownSum);

                                String echoData = objectMapper.writeValueAsString(echoMessage);

                                sendEchoMessageToParentAndController(socket, parentNode, echoData, ownIp, ownPort, controllerAdress, objectMapper, ownSum);
                            }


                        } else if (data.contains("echo")) {

                            neighborsInformed++;

                            EchoMessage echoMessage = objectMapper.readValue(data, EchoMessage.class);

                            ownSum += echoMessage.getPartialSum();

                            echoMessage = new EchoMessage(ownSum);

                            String echoData = objectMapper.writeValueAsString(echoMessage);

                            if (neighborsInformed == neighbors.size()) {
                                if (initiator) {

                                    ResultMessage resultMessage = new ResultMessage(ownSum);

                                    byte[] resultData = objectMapper.writeValueAsBytes(resultMessage);

                                    DatagramPacket resultDatagramPacket = new DatagramPacket(resultData, resultData.length, controllerAdress, controllerPort);

                                    System.out.println("Echo terminated");

                                    Thread.sleep((int) Math.random() * 100);
                                    socket.send(resultDatagramPacket);

                                } else {

                                    sendEchoMessageToParentAndController(socket, parentNode, echoData, ownIp, ownPort, controllerAdress, objectMapper, ownSum);
                                }
                            }
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    System.exit(0);
                }
            }).start();
        }
    }





    public static void sendInfoMessageToNeighborsAndController(DatagramSocket socket, List<ReducedNode> neighbors, String data, String ownIp, int ownPort, InetAddress controllerAdress, ObjectMapper objectMapper) throws IOException, InterruptedException {

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

            // Add latency
            Thread.sleep((int) (Math.random() * 100));

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

    public static void sendEchoMessageToParentAndController(DatagramSocket socket, ReducedNode parent, String data, String ownIp, int ownPort, InetAddress controllerAdress, ObjectMapper objectMapper, int partialSum) throws IOException, InterruptedException {

        // Get ip
        String parentIp = parent.getIp();

        // Get port
        int parentPort = parent.getPort();

        // Get address
        InetAddress neighborAddress = InetAddress.getByName(parentIp);

        // Create info packet
        DatagramPacket infoPacket = new DatagramPacket(data.getBytes(), data.length(), neighborAddress, parentPort);

        // Add latency
        Thread.sleep((int) (Math.random() * 100));

        // Send info packet
        socket.send(infoPacket);

        // Create info log message
        InfoLogMessage infoLogMessage = new InfoLogMessage(new Date().toString(), ownIp, ownPort, parentIp, parentPort, "echo", partialSum);

        // Convert log info message to bytes
        byte[] infoLogMessageAsByte = objectMapper.writeValueAsBytes(infoLogMessage);

        // create controller datagram packet
        DatagramPacket controllerDatagramPacket = new DatagramPacket(infoLogMessageAsByte, infoLogMessageAsByte.length, controllerAdress, controllerPort);

        socket.send(controllerDatagramPacket);

    }
    */
}
