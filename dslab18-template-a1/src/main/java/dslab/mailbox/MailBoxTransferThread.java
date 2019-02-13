package dslab.mailbox;

import dslab.Email;
import dslab.util.Config;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MailBoxTransferThread implements Runnable {

    private Socket socketFromTS;
    private ConcurrentHashMap<String, TreeMap<Integer, Email>> users;
    private Email email;

    private InputStream inputStream;
    private InputStreamReader inputStreamReader;
    private BufferedReader bufferedReader;
    private OutputStreamWriter outputStreamWriter;
    private BufferedWriter bufferedWriter;
    private OutputStream outputStream;
    private MailboxServer mailboxServer;
    private List<String> userList;
    private String domain;
    private Config usersProperties;


    public MailBoxTransferThread(Socket socketFromTS, ConcurrentHashMap<String, TreeMap<Integer, Email>> users, MailboxServer mailboxServer) {
        this.socketFromTS = socketFromTS;
        this.users = users;
        this.mailboxServer = mailboxServer;

        this.domain = mailboxServer.validDomain();
        this.usersProperties = new Config(mailboxServer.validConfig());
    }

    @Override
    public void run() {
        /**
         * Flag that is just used for "ok DMTP".
         */
        boolean flag = false;


        while (true) {
            try {
                // only enter scanning commands if the socket is still open
                if (!socketFromTS.isClosed()) {
                    if (!flag) {
                        sentResponseToClient("ok DMTP");
                        flag = true;
                    }

                    if (!socketFromTS.isClosed() && socketFromTS.getInputStream().available() > 0) {

                        // open stream and read incoming data
                        inputStream = socketFromTS.getInputStream();
                        inputStreamReader = new InputStreamReader(inputStream);
                        bufferedReader = new BufferedReader(inputStreamReader);

                        String recievedMsg = bufferedReader.readLine();
                        if (recievedMsg.startsWith("to") || recievedMsg.startsWith("from")){
                            recievedMsg = recievedMsg.replace(",","");
                        }
                        String[] command = recievedMsg.split("\\s+");

                        System.out.println("The following demand is forwarded : " + recievedMsg);
                        switch (command[0]) {
                            case "begin":
                                sentResponseToClient("ok");
                                this.email = new Email();
                                break;
                            case "to":
                                setRecipientsOfMail(command);
                                break;
                            case "from":
                                setSenderOfMail(command);
                                break;
                            case "subject":
                                setSubjectOfMail(command);

                                break;
                            case "data":
                                setDataOfMail(command);
                                break;
                            case "send":
                                sentResponseToClient("ok");
                                String user;
                                while (!userList.isEmpty()) {
                                    user = userList.remove(0);
                                    if (!users.containsKey(user)) {
                                        TreeMap<Integer, Email> firstMail = new TreeMap<>();
                                        firstMail.put(1, email);
                                        email.setMessageID(1);
                                        users.put(user, firstMail);
                                    } else {
                                        TreeMap<Integer, Email> nthMail = users.get(user);
                                        Integer last_id = nthMail.lastKey();
                                        nthMail.put(last_id + 1, email);
                                        email.setMessageID(last_id + 1);
                                        users.put(user, nthMail);
                                    }
                                }
                                break;
                            case "quit":
                                quitConnection(command);
                                break;
                            default:
                                sentResponseToClient("Error protocol error");
                                break;
                        }
                    } else if (socketFromTS.isClosed()) {
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


    public void setRecipientsOfMail(String command[]) {
        if (command.length < 2) {
            sentResponseToClient("error no recipient");
        }
        else {
            String isValid = checkIfUserOrDomainIsValid(command);
            if (!isValid.equals("ok")) {
                sentResponseToClient("error unknown recipient " + isValid);
            } else {
                String[] recipients = command[1].split(" ,");
                this.email.setRecipients(recipients);
                sentResponseToClient("ok " + (command.length - 1));
                  }
            }
        }

    private String checkIfUserOrDomainIsValid(String[] parts) {
        userList = new LinkedList<>();
        int i = 1;
        while (i < parts.length) {
            String[] recipients = parts[i].split("\\s+");
            for (String recipient : recipients) {
                int indexOf = recipient.indexOf("@");
                String userName = recipient.substring(0, indexOf);
                String userDomain = recipient.substring(indexOf + 1);

                if (!userDomain.equals(domain) || !usersProperties.containsKey(userName)) {
                    return userName;
                } else {
                    userList.add(userName);
                }
            }
            i++;
        }
        return "ok";
    }

    public void setSenderOfMail (String command []){
        if (command.length>2){
            sentResponseToClient("There is just one sender.");
        }else if (command.length==2) {
            email.setSender(command[1]);
            sentResponseToClient("ok");
        }else{
            sentResponseToClient("Error no sender");
        }
    }

    public void setSubjectOfMail(String command []) {
        if (command.length > 1) {
            StringBuilder subjectBuilder = new StringBuilder();
            int i = 1;
            while (i <command.length) {
                subjectBuilder.append(command[i]);
                subjectBuilder.append(" ");
                i++;
            }
            email.setSubject(subjectBuilder.toString());
            sentResponseToClient("ok");
        } else {
            sentResponseToClient("Error no subject");
        }
    }
    public void setDataOfMail(String command []) {
        if (command.length > 1) {
            int i = 1;
            StringBuilder dataBuilder = new StringBuilder();
            while (i <command.length) {
                dataBuilder.append(command[i]);
                dataBuilder.append(" ");
                i++;
            }
            email.setTextBody(dataBuilder.toString());
            sentResponseToClient("ok");
        } else {
            sentResponseToClient("Error no data");
        }
    }



    public void quitConnection(String command []){
        if (command[0].equals("quit")){
            sentResponseToClient("ok bye");
        }else {
            sentResponseToClient("Error protocol error");
        }
    }



    /**
     * Sends response message to client.
     * @param response response message.
     */
    private void sentResponseToClient(String response) {
        try {
            outputStream = socketFromTS.getOutputStream();

            outputStreamWriter = new OutputStreamWriter(outputStream);

            bufferedWriter = new BufferedWriter(outputStreamWriter);
            bufferedWriter.write(response + "\n");
            bufferedWriter.flush();
            if (response.equals("ok bye") || response.equals("Invalid command") || response.equals("Error protocol error")) {
                socketFromTS.close();
                bufferedReader.close();
                bufferedWriter.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
