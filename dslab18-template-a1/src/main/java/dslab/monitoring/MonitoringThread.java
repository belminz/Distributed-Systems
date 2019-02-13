package dslab.monitoring;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Arrays;
import java.util.HashMap;

public class MonitoringThread extends Thread {
    private HashMap<String,Integer> servers;
    private DatagramSocket dataSocket;
    private HashMap<String,Integer> addresses;



    public MonitoringThread(DatagramSocket dataSocket, HashMap<String,Integer> addresses, HashMap<String,Integer> servers){
        this.servers = servers;
        this.addresses = addresses;
        this.dataSocket = dataSocket;
    }

    public void run() {
        DatagramPacket packet;
        try {
            while (true) {
                byte [] dataBuffer = new byte[2014];
                // create a datagram packet of (buffer.length)

                packet = new DatagramPacket(dataBuffer, dataBuffer.length);
                //try to recieve packets from client
                dataSocket.receive(packet);
                // get the data from the packet
                String getDataFromPacket = new String(packet.getData()).trim();
                String[] commandParts = getDataFromPacket.split("\\s+");
                System.out.println("Recieved packet from transfer server " + getDataFromPacket);

                String server = commandParts[0];
                String address = commandParts[1];

                if (!servers.isEmpty()) {
                    if (servers.containsKey(server)) {
                        servers.put(server, servers.get(server)+1);
                    } else if(!servers.containsKey(server)) {
                        servers.put(server, 1);
                    }
                }
                else {
                    servers.put(server,1);
                }

                if (!addresses.isEmpty()) {
                    if (addresses.containsKey(address)) {
                        addresses.put(address, addresses.get(address)+1);
                    }
                    else{
                        addresses.put(address, 1);
                    }
                }
                else {
                    addresses.put(address,1);
                }

            }
        } catch (SocketException socEx) {
            System.out.println( socEx.getMessage());
        } catch (IOException ioe) {
            System.out.println(ioe.toString());
        } finally {
            if (!dataSocket.isClosed() && dataSocket != null ) {
                dataSocket.close();
            }
        }
    }
}
