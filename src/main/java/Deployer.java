import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Deployer {

    public static final String controllerIp = "192.168.3.2";
    public static final int controllerPort = 50000;

    public static final String currentDir = System.getProperty("user.dir");


    public static void main(String[] args) {

        new Deployer().start();
    }

    public void start() {

        List<Process> nodeProcesses = null;

        try {
            // Create object mapper
            ObjectMapper objectMapper = new ObjectMapper();

            // Path to my network
            Path path = Paths.get(currentDir + File.separator + "mynet.json");

            // Create buffered reader
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

            // Get all the nodes from the node network
            List<Node> nodes = nodeNetwork.getNodes();

            // Create datagram socket for the controller
            DatagramSocket controllerSocket = new DatagramSocket(controllerPort);

            // Start all the nodes
            // nodeProcesses = startNodes(nodes);

            // Start remote processes
            startRemoteProcesses(nodes);

            // Start the controller
            new Controller().startController(nodes, controllerSocket, nodeNetwork);

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

    /**
     * Starts all the nodes in a seperate process
     * @param nodes The nodes to start
     * @return A list of Processes for cleanup
     */
    public List<Process> startNodes(List<Node> nodes) {

        List<Process> processes = new ArrayList<>();

        File directory = new File(currentDir);

        File[] files = directory.listFiles();

        boolean fileExists = false;

        for (File file : files) {
            if (file.getName().contains("VerteileSystemeLab3Node.jar")) {
                fileExists = true;
                break;
            }
        }

        if (!fileExists) {
            System.err.println("Insert VerteileSystemeLab3Node.jar into the current directory");

            System.exit(-1);
        }

        String jarPath = currentDir + File.separator + "VerteileSystemeLab3Node.jar";


        for (Node node : nodes) {

            String myIp = node.getIp();

            int myPort = node.getPort();

            String ipsAndPortsArgs = parseIpAndPortsToStringArg(node.getNeighbors());

            int memory = node.getNodeId();

            // Create process builder with the needed arguments
            ProcessBuilder processBuilder = new ProcessBuilder("java", "-jar", jarPath, myIp, String.valueOf(myPort), ipsAndPortsArgs, String.valueOf(memory));

            try {
                // Start the process and add to list
                Process process = processBuilder.start();
                processes.add(process);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return processes;
    }

    /**
     * Starts n remote processes, needs the startnode script
     * @param nodes
     * @throws IOException
     */
    public void startRemoteProcesses(List<Node> nodes) throws IOException {

        for (Node node : nodes) {

            String nodeIp = node.getIp();

            int nodePort = node.getPort();

            List<ReducedNode> neighbors = node.getNeighbors();

            String neighborsArgs = parseIpAndPortsToStringArg(neighbors);

            int initialMemory = node.getNodeId();

            String nodeLocation = "/home/student/lab3/startnode";

            String sshCommand = "ssh student@" + nodeIp + " '" + nodeLocation + " " + nodeIp +
                    " " + String.valueOf(nodePort) + " " + neighborsArgs + " " + String.valueOf(initialMemory) + " &'";

            Process process = Runtime.getRuntime().exec(new String[]{"bash", "-c", sshCommand});
        }
    }

    /**
     * Parses the list of neighbors into a String argument for the process builder
     * @param neighbors The neighbors
     * @return String argument for the process builder
     */
    public String parseIpAndPortsToStringArg(List<ReducedNode> neighbors) {

        StringBuilder ipsAndPortsArgs = new StringBuilder();

        for (ReducedNode neighbor : neighbors) {

            ipsAndPortsArgs.append(neighbor.getIp() + ":" + neighbor.getPort() + "\\;");
        }

        return ipsAndPortsArgs.toString();
    }
}
