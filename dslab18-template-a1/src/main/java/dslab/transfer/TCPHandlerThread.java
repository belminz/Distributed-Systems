package dslab.transfer;

import dslab.Email;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TCPHandlerThread implements Runnable {


    /**
     * executes new Threads in order to connect to clients.
     * For every client there is new Thread that is executed.
     */
    private ExecutorService tcpClientThreadPool = Executors.newCachedThreadPool();

    /**
     * ServerSocket to listen to incoming connections
     */
    private ServerSocket serverSocket;
    /**
     * When ServerSocket receives a connection, the client
     * connection is stored in this Socket
     */
    private Socket clientSocket;

    /**
     * emails to be forwarded
     */
    private BlockingQueue<Email> emails;

    /**
     *
     * @param serverSocket checks if serverSocket is opened or not
     * @param emails that are going to be forwarded
     */
    public TCPHandlerThread(ServerSocket serverSocket, BlockingQueue<Email> emails) {
        this.serverSocket = serverSocket;
        this.emails = emails;
    }

    /**
     * this server accepts new connections from clients side.
     */
    @Override
    public void run() {
        while(true) {
            if (!serverSocket.isClosed()) {
                try {
                    clientSocket = this.serverSocket.accept();

                } catch (IOException e) {
                    tcpClientThreadPool.shutdownNow();
                    try {
                        if (clientSocket != null) {
                            clientSocket.close();
                        }
                    } catch (IOException ioe) {
                        System.out.println("Cannot  close Client Socket" + ioe.getMessage());
                    }
                }
            }
            if (!serverSocket.isClosed()) {
                this.tcpClientThreadPool.execute(new TCPClientThread(clientSocket, emails));
            } else {
                this.tcpClientThreadPool.shutdown();
            }
        }
    }
}

