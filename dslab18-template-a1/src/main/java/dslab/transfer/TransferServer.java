package dslab.transfer;

import java.io.*;
import java.net.ServerSocket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import dslab.ComponentFactory;
import dslab.Email;
import dslab.util.Config;

public class TransferServer implements ITransferServer, Runnable {

    /**
     * Creates a new server instance.
     *
     * @param componentId the id of the component that corresponds to the Config resource
     * @param config the component config
     * @param in the input stream to read console input from
     * @param out the output stream to write console output to
     */

    /**
     * ServerSocket to listen to incoming connections
     */

    private ServerSocket serverSocket;
    /**
     *  port numbers from transfer.properties
     */

    private int tcpPort;

    /**
     * Two threads for two separate connections, TCPHandlerThread (Listening) and ServerForForwarding(forwarding)
     */
    private ExecutorService executorService = Executors.newFixedThreadPool(2);

    /**
     * BufferedReader used to read commands from System.in
     */
    private BufferedReader bufferedReader;

    /**
     * created emails to be forwarded to mailbox server via thread for forwarding
     */
    BlockingQueue<Email> emailsToBeForwarded = new LinkedBlockingQueue<>();



    private String componentId;
    private Config config;
    private InputStream in;
    private PrintStream out;



    public TransferServer(String componentId, Config config, InputStream in, PrintStream out) {
        // TODO
        this.componentId = componentId;
        this.config = config;
        this.in = in;
        this.out = out;

        this.tcpPort = this.config.getInt("tcp.port");
    }

    @Override
    public void run() {
        openServerSocket();
        executorService.execute(new TCPHandlerThread(serverSocket, emailsToBeForwarded));
        executorService.execute(new ServerForEmailsForwarding(emailsToBeForwarded, config));
        System.out.println("Transfer server is now started.");

        try {
            bufferedReader = new BufferedReader(new InputStreamReader(in));
            if (bufferedReader.readLine().equals("shutdown")){
                shutdown();
            }
        } catch (IOException e) {
            e.printStackTrace();
            executorService.shutdownNow();
        }
    }

    @Override
    public void shutdown() {
        if (serverSocket != null) {
            stopServer();
            executorService.shutdown();
        }
    }
    /**
     * opens ServerSocket to listen to incoming tcp connections,
     * instantiates serverSocket with the given port
     */

    private void openServerSocket() {
        try {
            this.serverSocket = new ServerSocket(tcpPort);
        } catch (IOException ioe) {
            throw new RuntimeException("Cannot open port", ioe);

        }
    }
    /**
     * Stops the server and closing the sockets.
     */
    public synchronized void stopServer(){
        try {
            if (!serverSocket.isClosed()){
            this.serverSocket.close();
        }
        executorService.shutdownNow();
        }
        catch (IOException e) {
            executorService.shutdown();
        }
    }


    public static void main(String[] args) throws Exception {
        ITransferServer server = ComponentFactory.createTransferServer(args[0], System.in, System.out);
        server.run();
    }

}
