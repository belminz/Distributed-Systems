package dslab.mailbox;

import dslab.Email;
import dslab.util.Config;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TCPMailHandlerThread implements Runnable {

    /**
     * When ServerSocket receives a connection, the client
     * connection is stored in this Socket
     */
    private Socket clientSocket;
    private MailboxServer mailboxServer;
    /**
     * ServerSocket to listen to incoming connections
     */
    private ServerSocket serverSocket;


    private ConcurrentHashMap<String, TreeMap<Integer,Email>> users;

    private Config config;
    private boolean dmapProtocol;
    private ExecutorService tcpMailThreadPool = Executors.newCachedThreadPool();

    /**
     * @param users Stores users, and its ids and mails.
     * @param config (valid) configurations of users
     * @param serverSocket server socket that is used to be sure its still working
     * @param dmapProtocol boolean for DMAP or DMTP protocol (true=DMAP)
     * @param mailboxServer checks valid domains/config. of users.
     */

    public TCPMailHandlerThread (ConcurrentHashMap<String,TreeMap<Integer,Email>> users,Config config ,ServerSocket serverSocket,boolean dmapProtocol,MailboxServer mailboxServer){
        this.users  = users;
        this.config = config;
        this.serverSocket = serverSocket;
        this.dmapProtocol = dmapProtocol;
        this.mailboxServer = mailboxServer;
    }


    @Override
    public void run() {
        while(true) {
            if (!serverSocket.isClosed()) {
                try {
                    //Waiting for incoming connections.
                    clientSocket = this.serverSocket.accept();
                    if (dmapProtocol){
                        this.tcpMailThreadPool.execute(new MailboxClientThread(clientSocket, config, users));
                    }else {
                        this.tcpMailThreadPool.execute(new MailBoxTransferThread(clientSocket,users,this.mailboxServer));
                    }
                }
                catch (IOException e) {
                    try {
                        // close the socket if it is opened
                        if (clientSocket != null) {
                            clientSocket.close();
                        }
                    } catch (IOException ioe) {
                        System.out.println("Cannot  close Client Socket" + ioe.getMessage());
                    }
                    // shutdown thread pool
                    this.tcpMailThreadPool.shutdown();
                }
            }
        }
    }
}
