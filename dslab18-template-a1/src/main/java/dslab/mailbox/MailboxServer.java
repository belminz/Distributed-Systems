package dslab.mailbox;

import java.io.*;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import dslab.ComponentFactory;
import dslab.Email;
import dslab.util.Config;

public class MailboxServer implements IMailboxServer, Runnable {

    /**
     * Creates a new server instance.
     *
     * @param componentId the id of the component that corresponds to the Config resource
     * @param config the component config
     * @param in the input stream to read console input from
     * @param out the output stream to write console output to
     */

    private String componentId;
    private Config config;
    private InputStream in;
    private PrintStream out;

    /**
     * ServerSocket to listen to incoming connections
     *  2 types of sockets, one for access protocol, one for exchange protocol
     */

    private ServerSocket socketDMAP;
    private ServerSocket socketDMTP;

    /**
     * Two threads for two separate connections, one for listen to user connections,
     * one to listen transferserver connections
     */
    private ExecutorService executorService = Executors.newFixedThreadPool(2);

    /**
     * BufferedReader used to read commands from System.in
     */
    private BufferedReader bufferedReader;


    private int dmapPort;

    private int dmtpPort;

    /**
     * list of users in one hash map.
     */
    private ConcurrentHashMap<String, TreeMap<Integer,Email>> users = new ConcurrentHashMap<>();


    public MailboxServer(String componentId, Config config, InputStream in, PrintStream out) {
        this.componentId = componentId;
        this.config = config;
        this.in = in;
        this.out = out;

        this.dmapPort = this.config.getInt("dmap.tcp.port");
        this.dmtpPort = this.config.getInt("dmtp.tcp.port");
    }

    public String validDomain(){
        return this.config.getString("domain");
    }
    public String validConfig(){
        return this.config.getString("users.serverConfig");
    }

    @Override
    public void run() {
        openServerSocketDMAP();
        openServerSocketDMTP();
        executorService.execute(new TCPMailHandlerThread(users,config,socketDMAP,true,this));
        executorService.execute(new TCPMailHandlerThread(users,config,socketDMTP,false,this));


        System.out.println("Mailbox server is now started.");

        bufferedReader = new BufferedReader(new InputStreamReader(in));
        try {
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
        try {
            if (!socketDMTP.isClosed()) {
                socketDMTP.close();
            }
            if (!socketDMAP.isClosed()) {
                socketDMAP.close();
            }
            executorService.shutdownNow();
        }catch (IOException e){
            executorService.shutdownNow();
        }
    }

    /**
     * opens ServerSocket to listen to incoming tcp connections,
     * instantiates serverSocket with the given port
     */

    private void openServerSocketDMAP() {
        try {
            this.socketDMAP = new ServerSocket(dmapPort);
        } catch (IOException ioe) {
            throw new RuntimeException("Cannot open port", ioe);

        }
    }

    /**
     * opens ServerSocket to listen to incoming tcp connections,
     * instantiates serverSocket with the given port
     */

    private void openServerSocketDMTP() {
        try {
            this.socketDMTP = new ServerSocket(dmtpPort);
        } catch (IOException ioe) {
            throw new RuntimeException("Cannot open port", ioe);

        }
    }

    public static void main(String[] args) throws Exception {
        IMailboxServer server = ComponentFactory.createMailboxServer(args[0], System.in, System.out);
        server.run();
    }
}
