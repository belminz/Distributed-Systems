package dslab.transfer;
import dslab.Email;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Logger;


public class TCPClientThread implements Runnable {

    private Email email;
    private Socket clientSocket;
    private InputStream inputStream;
    private InputStreamReader inputStreamReader;
    private BufferedReader bufferedReader;
    private OutputStreamWriter outputStreamWriter;
    private BufferedWriter bufferedWriter;
    private OutputStream outputStream;
    private BlockingQueue<Email> emails;
    int count = 0;

    public TCPClientThread(Socket clientSocket,BlockingQueue<Email> emails) {
        this.clientSocket = clientSocket;
        this.emails = emails;
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
                if (!clientSocket.isClosed()) {

                    if (!flag) {
                        sentResponseToClient("ok DMTP");
                        flag = true;
                    }

                    if (!clientSocket.isClosed() && clientSocket.getInputStream().available() > 0) {

                        // open stream and read incoming data
                        inputStream = clientSocket.getInputStream();
                        inputStreamReader = new InputStreamReader(inputStream);
                        bufferedReader = new BufferedReader(inputStreamReader);

                        String request = bufferedReader.readLine();
                        if (request.startsWith("to") || request.startsWith("from")){
                            request = request.replace(",","");
                        }

                        String[] command = request.split("\\s+");
                        switch (command[0]) {

                            case "begin":
                                if (count == 0) {
                                    sentResponseToClient("ok");
                                    count++;
                                    this.email = new Email();
                                }
                                break;
                            case "to":
                                if (count == 1) {
                                    setRecipientsOfMail(command);
                                }
                                break;
                            case "from":
                                if (count == 1){
                                    setSenderOfMail(command);
                                }
                                break;
                            case "subject":
                                if (count == 1){
                                    setSubjectOfMail(command);
                                }
                                break;
                            case "data":
                                if (count == 1){
                                    setDataOfMail(command);
                                }
                                break;
                            case "send":
                                if (count==1){
                                    sendMailToRecievers(command);
                                    emails.add(email);
                             //       System.out.println("Email sent");
                                }
                                break;
                            case "quit":
                                quitConnection(command);
                                break;
                            default:
                                sentResponseToClient("Error protocol error");
                                break;
                        }
                    } else if (clientSocket.isClosed()) {
                        bufferedReader.close();
                        bufferedWriter.close();
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
    if (command.length > 1) {
        email.setRecipients(Arrays.copyOfRange(command,1,command.length));
        sentResponseToClient("ok " + (command.length - 1));
    } else {
        sentResponseToClient("Error no reciever");
    }
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

    public void sendMailToRecievers(String command []) {
        if (email.getRecipients() == null) {
            sentResponseToClient("error, there are no recipient/s of email");
        } else if (email.getSender() == null) {
            sentResponseToClient("There is no sender of email");
        } else {
            if (command.length == 1) {
                sentResponseToClient("ok");
                count --;
            }else {
                sentResponseToClient("Send wrong format");
            }
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
