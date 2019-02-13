package dslab.transfer;

import dslab.Email;
import dslab.util.Config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.util.*;
import java.util.concurrent.BlockingQueue;

public class ServerForEmailsForwarding implements Runnable {

    private BlockingQueue<Email> emails;
    private Config serverConfig;
    private Email email;
    private Socket socket;
    private Config config = new Config("domains.properties");

    public ServerForEmailsForwarding(BlockingQueue<Email> emails, Config serverConfig) {

        this.emails = emails;
        this.serverConfig = serverConfig;
    }

    public void connectToMailboxServer(String [] recipients,String host, String port) {
        try {
            socket = new Socket(host, Integer.parseInt(port));
            // create a reader to retrieve messages send by the server
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            // create a writer to send messages to the server
            PrintWriter printWriter = new PrintWriter(socket.getOutputStream());
            System.out.println("Socket For Forwarding is now activated");
            String receivedMessage;


            if ((receivedMessage = bufferedReader.readLine()) != null) {
                if (receivedMessage.equals("ok DMTP")) {
                    printWriter.println("begin");
                    printWriter.flush();
                }
            }

            if ((receivedMessage = bufferedReader.readLine()) != null) {
                if (receivedMessage.equals("ok")) {
                    printWriter.println("to " + Arrays.toString(recipients).replace(",", "")
                            .replace("[", "")
                            .replace("]", "")
                            .trim());
                    printWriter.flush();
                }
            }


            if ((receivedMessage = bufferedReader.readLine()) != null) {
                if (receivedMessage.startsWith("ok")) {
                    printWriter.println("from " + email.getSender());
                    printWriter.flush();
                }
            }

            if ((receivedMessage = bufferedReader.readLine()) != null) {
                if (receivedMessage.equals("ok")) {
                    printWriter.println("subject " + email.getSubject());
                    printWriter.flush();
                }
            }

            if ((receivedMessage = bufferedReader.readLine()) != null) {
                if (receivedMessage.equals("ok")) {
                    printWriter.println("data " + email.getTextBody());
                    printWriter.flush();
                }
            }



            if ((receivedMessage = bufferedReader.readLine()) != null) {
                if (receivedMessage.equals("ok")) {
                    printWriter.println("send");
                    printWriter.flush();
                }
            }


            if ((receivedMessage = bufferedReader.readLine()) != null) {
                if (receivedMessage.equals("ok")) {
                    printWriter.println("quit");
                    printWriter.flush();

                    /**
                     * Connection to monitor server.
                     */

                    System.out.println("Sending data to monitor server");
                    DatagramSocket datagramSocket = new DatagramSocket();
                    String message = "127.0.0.1:" + serverConfig.getString("tcp.port") + " " +  email.getSender();
                    byte[] buffer = message.getBytes();
                    DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(serverConfig.getString("monitoring.host")), serverConfig.getInt("monitoring.port"));
                    datagramSocket.send(datagramPacket);


                }
            }

            if((receivedMessage = bufferedReader.readLine()) != null){
                if (receivedMessage.equals("ok bye")){
                    bufferedReader.close();
                    printWriter.close();
                    socket.close();
                }
            }
        } catch (UnknownHostException uHe) {
            System.out.println("Connection to host is not sucessfull " + uHe.getMessage());
        } catch (SocketException se) {
            System.out.println("Socket exeption" + se.getMessage());
        } catch (IOException ioe) {
            System.out.println(ioe.toString());
        } finally {
            if (!socket.isClosed() && socket != null) {
                try {
                    socket.close();
                    System.out.println("Socket For Forwarding is now closed.");
                } catch (IOException e) {
                    //   System.out.println(e.toString());
                }
            }
        }
    }


    @Override
    public void run() {
        while (true){
                Queue<String> alreadyVisitedDomains = new LinkedList<>();
                try {
                    email = emails.take();
                      System.out.println("email to be sent: "+email.toString());
                    for (String recipients : email.getRecipients()) {
                        String dom = recipients.substring(recipients.indexOf("@") + 1);
                        if (config.containsKey(dom)) {
                            if (!alreadyVisitedDomains.contains(dom)) {
                                String hostAndPortValues = config.getString(dom);
                                String host = hostAndPortValues.substring(0, hostAndPortValues.indexOf(":"));
                                String port = hostAndPortValues.substring(hostAndPortValues.indexOf(":") + 1);
                                alreadyVisitedDomains.add(dom);
                                System.out.println("Valid domain: " + dom);
                                connectToMailboxServer(email.getRecipients(), host, port);
                            }
                        } else {
                            //Message delivery failure
                            System.out.println("Invalid domain");
                            String domain = email.getSender().substring(email.getSender().indexOf("@") + 1);
                            String[] deliverBack = new String[1];
                            deliverBack[0] = email.getSender();
                            Email email = new Email();
                            if (config.containsKey(domain)) {
                                email.setRecipients(deliverBack);
                                email.setSender("mailer@" + serverConfig.getString("monitoring.host"));
                                email.setSubject("Mail delivery Failure");
                                email.setTextBody("Recipient/s you entered has/have invalid domain.");
                                emails.add(email);
                            } else {
                                System.out.println("Sender domain invalid,error "+ domain);
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    break;
                }
            }
    }
}
