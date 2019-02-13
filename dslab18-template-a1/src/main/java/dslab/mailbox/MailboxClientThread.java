package dslab.mailbox;

import dslab.Email;
import dslab.util.Config;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MailboxClientThread implements Runnable {

    private Config config;
    private ConcurrentHashMap<String, TreeMap<Integer,Email>> users;

    private Socket clientSocket;
    private InputStream inputStream;
    private InputStreamReader inputStreamReader;
    private BufferedReader bufferedReader;
    private OutputStreamWriter outputStreamWriter;
    private BufferedWriter bufferedWriter;
    private OutputStream outputStream;
    int count = 0;
    private Config configurationOfUser;
    private String username ;
  //  private List userList ;

    public MailboxClientThread(Socket clientSocket, Config config, ConcurrentHashMap<String, TreeMap<Integer,Email>> users) {
        this.clientSocket = clientSocket;
        this.config = config;
        this.users = users;

        //Delete .properties
        String key = config.getString("users.serverConfig");
        if (key.contains(".properties")) {
            String data[] = key.split(".properties");
            key = data[0];
        }
        this.configurationOfUser = new Config(key);
       //     this.userList = new LinkedList();
    }

    @Override
    public void run() {
/**
 * Flag that is just used for "ok DMAP".
 */
        boolean flag = false;


while (true) {
    try {
        // only enter scanning commands if the socket is still open
        if (!clientSocket.isClosed()) {
            if (!flag) {
                sendResponse("ok DMAP");
                flag = true;
            }
            if (!clientSocket.isClosed() && clientSocket.getInputStream().available() > 0) {

                // open stream and read incoming data
                inputStream = clientSocket.getInputStream();
                inputStreamReader = new InputStreamReader(inputStream);
                bufferedReader = new BufferedReader(inputStreamReader);

                String request = bufferedReader.readLine();
                String[] command = request.split(" ");
                switch (command[0]) {
                    case "login":
                        if (count == 0) {
                            if (command.length==3){
                                //do login
                                String username = command[1];
                                String password = command[2];
                                if (configurationOfUser.containsKey(username)) {
                                    if (configurationOfUser.listKeys().contains(username) && configurationOfUser.getString(username).equals(password)) {
                                        count++;
                                        sendResponse("ok");
                                        this.username = username;
                                    } else {
                                        sendResponse("error wrong password");
                                    }
                                } else {
                                    sendResponse("error wrong username");
                                }
                            }
                            else {
                                sendResponse("error invalid login format");
                        }
                    } else if (count ==1){
                            sendResponse("error already logged in!");
                      }
                        break;
                    case "list":
                        if (count == 1) {
                            if (command.length == 1) {
                                if (users.isEmpty()){
                                    sendResponse("Mailbox empty.");
                                }
                                else{
                                    //do listing
                                    String response=null;
                                    boolean isEmpty = true;
                                    TreeMap<Integer, Email> idAndMail = users.get(username);

                                    List<Integer> listofIds = new LinkedList<>(idAndMail.keySet());
                                    StringBuilder responseBuilder = new StringBuilder();

                                    for (int i = 0;i<idAndMail.size();i++) {
                                        for (int j = 0; j < listofIds.size(); j++) {
                                            Integer id = listofIds.remove(0);
                                            Email email = users.get(username).get(id);
                                            if (response == null) {
                                                response = responseBuilder.append(id).append(" ").append(email.getSender()).append(" ").append(email.getSubject()).toString();
                                            } else {
                                                responseBuilder.append(System.lineSeparator());
                                                response = responseBuilder.append(id).append(" ").append(email.getSender()).append(" ").append(email.getSubject()).toString();
                                            }
                                            isEmpty = false;
                                        }
                                    }
                                    if (isEmpty){
                                        response = "Mailbox empty.";
                                    }
                                    sendResponse(response);
                            }
                            } else {
                                sendResponse("error invalid format for -list-");
                            }
                        }else {
                            sendResponse("error not logged in");
                        }
                        break;
                    case "show":
                        if (count == 1) {
                            if (command.length==2) {
                                Integer id = Integer.parseInt(command[1]);
                                if (users.isEmpty()) {
                                    sendResponse("Mailbox empty.");
                                } else {
                                    //else show mail
                                    StringBuilder showMail = new StringBuilder();
                                    String response = "";
                                    Email email = users.get(username).get(id);
                                    if (email!=null) {
                                        response +="from " +  showMail.append(email.getSender())
                                                .append(System.lineSeparator())
                                                .append("to ").append(Arrays.toString(email.getRecipients()).replace("[", "").replace("]", ""))
                                                .append(System.lineSeparator())
                                                .append("subject ").append(email.getSubject())
                                                .append(System.lineSeparator())
                                                .append("data ").append(email.getTextBody()).toString();
                                    } else {
                                        response = "Mailbox empty.";
                                    }
                                    sendResponse(response);
                                }
                                }
                            else {
                                sendResponse("error invalid format for -show-");
                            }
                        }
                        else {
                            sendResponse("error not logged in");
                        }
                        break;
                    case "delete":
                        if (count == 1) {
                            if (command.length==2){
                            Integer id = Integer.parseInt(command[1]);
                                if (users.isEmpty()) {
                                    sendResponse("Mailbox empty. Nothing to delete.");
                                } else {
                                   //do delete
                                    String response = "";
                                    Email email = users.get(username).get(id);
                                    TreeMap<Integer, Email> ids = users.get(username);
                                    if (email != null) {
                                        if (ids.containsKey(id)) {
                                            if (email.getMessageID().equals(id)) {
                                                users.get(username).remove(id);
                                                response = "ok";
                                            }
                                        } else {
                                            response = "error unknown message id";
                                        }
                                    } else {
                                        response = "error unknown message id";

                                    }
                                  sendResponse(response);
                                }
                            } else {
                            sendResponse("error invalid format for -delete-");
                            }
                        } else{
                            sendResponse("error not logged in");
                        }
                        break;
                    case "logout":
                        if (count==1){
                            if (command.length==1){
                                count--;
                                sendResponse("ok");
                            }
                        } else {
                            sendResponse("error not logged in");
                        }
                        break;
                    case "quit":
                        sendResponse("ok bye");
                        bufferedReader.close();
                        clientSocket.close();
                        break;
                    default:
                        sendResponse("error invalid command!");
                    break;
                }

            } else if (clientSocket.isClosed()) {
                bufferedReader.close();
                break;
            }
        } else {
            break;
        }
    } catch (IOException ioe) {
        System.out.println(ioe.getMessage());
    }
  }
}

    /**
     * Sends response message to client.
     * @param response response message.
     */
    private void sendResponse(String response) {
        try {
            outputStream = clientSocket.getOutputStream();
            outputStreamWriter = new OutputStreamWriter(outputStream);
            bufferedWriter = new BufferedWriter(outputStreamWriter);
            bufferedWriter.write(response + "\n");
            bufferedWriter.flush();
            if (response.equals("ok bye") || response.equals("Invalid command") || response.equals("Error protocol error")) {
                clientSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
