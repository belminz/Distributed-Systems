package dslab;


import java.util.Arrays;

public class Email {

private String sender;
private String [] recipients;
private String subject;
private String TextBody;
private Integer messageID;


    public Integer getMessageID() {
        return messageID;
    }

    public void setMessageID(Integer messageID) {
        this.messageID = messageID;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String[] getRecipients() {
        return recipients;
    }

    public void setRecipients(String[] recipients) {
        this.recipients = recipients;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getTextBody() {
        return TextBody;
    }

    public void setTextBody(String textBody) {
        TextBody = textBody;
    }


    @Override
    public String toString() {
        return "Email{" +
                "sender='" + sender + '\'' +
                ", recipients=" + Arrays.toString(recipients) +
                ", subject='" + subject + '\'' +
                ", TextBody='" + TextBody + '\'' +
                ", messageID=" + messageID +
                '}';
    }
}


