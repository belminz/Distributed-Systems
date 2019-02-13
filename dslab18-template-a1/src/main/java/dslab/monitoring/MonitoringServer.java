package dslab.monitoring;

import java.io.*;
import java.net.DatagramSocket;

import java.util.HashMap;


import dslab.ComponentFactory;
import dslab.util.Config;

public class MonitoringServer implements IMonitoringServer {


    private String componentId;
    private Config config;
    private InputStream in;
    private PrintStream out;
    private DatagramSocket datagramSocket;
    private HashMap<String, Integer> servers = new HashMap<>();
    private HashMap<String, Integer> addresses = new HashMap<>();
    /**
     * Creates a new server instance.
     *
     * @param componentId the id of the component that corresponds to the Config resource
     * @param config the component config
     * @param in the input stream to read console input from
     * @param out the output stream to write console output to
     */
    public MonitoringServer(String componentId, Config config, InputStream in, PrintStream out) {
        this.componentId = componentId;
        this.config = config;
        this.in = in;
        this.out = out;
    }

    @Override
    public void run() {
        try {
            datagramSocket = new DatagramSocket(config.getInt("udp.port"));
            new MonitoringThread(datagramSocket,addresses,servers).start();

        } catch (IOException e){
            throw new RuntimeException("Cannot  listen on UDP port.",e);
        }

        System.out.println("Monitoring Server is up!");

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(in));

        try {
            label:
            while (true) {
                //testing..
                switch (bufferedReader.readLine()) {
                    case "shutdown":
                        shutdown();
                        break label;
                    case "addresses":
                        addresses();
                        break;
                    case "servers":
                        servers();
                        break;
                    default:
                        out.println("invalid command!");
                        break;
                }
            }
        } catch (IOException e){
            System.err.println("IO Error!");
        }

    }




    @Override
    public void addresses() {
       addressesAndServers(addresses);
    }

    @Override
    public void servers() {
        addressesAndServers(servers);
    }

    @Override
    public void shutdown() {
        if (datagramSocket != null) {
            datagramSocket.close();
      }
    }

    public void addressesAndServers (HashMap<String,Integer> hashMap){
        for (String key : hashMap.keySet()){
            out.println(key + " " + hashMap.get(key));
            out.flush();
        }

    }


    public static void main(String[] args) throws Exception {
        IMonitoringServer server = ComponentFactory.createMonitoringServer(args[0], System.in, System.out);
        server.run();
    }

}
