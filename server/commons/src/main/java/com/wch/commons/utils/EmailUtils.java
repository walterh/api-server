package com.wch.commons.utils;

import java.security.Security;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class EmailUtils {
    private static final String SSL_FACTORY = "javax.net.ssl.SSLSocketFactory";

    // hard-coded SMTP parameters for delegate account
    private static final String sSmtpServer = "smtp.gmail.com";
    private static final int sSmtpPort = 465; // 587;
    public static final String SMTP_DEFAULT_SENDER = "llug.emailer@gmail.com";
    private static final String sSmtpPassword = "6789878767653";
    public static String SMTP_DEFAULT_RECIPIENT = null;

    static final Pattern EMAIL_PATTERN = Pattern.compile("^([a-zA-Z0-9_.+-]+)@([a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+)$");
    static final int MAX_USERNAME_LENGTH = 128;
    static final int MAX_DOMAIN_NAME_LENGTH = 256;

    private static int sSecurityProvider = Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());

    public static boolean validateEmail(String email) {
        Matcher m = EMAIL_PATTERN.matcher(email);
        boolean matched = m.matches();
        String userName = matched ? m.group(1) : "";
        String hostName = matched ? m.group(2) : "";
        boolean nameTooLong = userName.length() > MAX_USERNAME_LENGTH;
        boolean hostTooLong = hostName.length() > MAX_DOMAIN_NAME_LENGTH;

        return matched && !nameTooLong && !hostTooLong;
    }

    public static void main(String args[]) throws Exception {
        String sendTo = "walteusmaximus@live-int.com";
        String emailSubjectTxt = "foo subject";
        String emailMsgTxt = "foo body";
        String emailFromAddress = SMTP_DEFAULT_SENDER;

        EmailUtils.sendSslMessage(new String[] { sendTo }, emailSubjectTxt, emailMsgTxt, emailFromAddress);
        System.out.println("Sucessfully Sent mail to All Users");
    }

    public static void sendSslMessage(String recipients[], String subject, String message, String from) {
        boolean debug = true;
        try {

            Properties props = new Properties();
            props.put("mail.transport.protocol", "smtps");
            props.put("mail.smtps.host", sSmtpServer);
            props.put("mail.smtps.auth", "true");

            if (from == null) {
                from = SMTP_DEFAULT_SENDER;
            }
            if (recipients == null) {
                recipients = new String[] { SMTP_DEFAULT_RECIPIENT };
            }

            Session mailSession = Session.getDefaultInstance(props);
            mailSession.setDebug(true);
            Transport transport;
            transport = mailSession.getTransport();
            MimeMessage msg = new MimeMessage(mailSession);
            InternetAddress addressFrom = new InternetAddress(from);
            msg.setFrom(addressFrom);

            InternetAddress[] addressTo = new InternetAddress[recipients.length];

            for (int i = 0; i < recipients.length; i++) {
                addressTo[i] = new InternetAddress(recipients[i]);
            }
            msg.setRecipients(Message.RecipientType.TO, addressTo);

            // Setting the Subject and Content Type
            msg.setSubject(subject);
            msg.setContent(message, "text/plain");

            /*
            transport.connect(sSmtpServer, sSmtpPort, SMTP_DEFAULT_SENDER, sSmtpPassword);

            transport.sendMessage(msg, msg.getRecipients(Message.RecipientType.TO));
            transport.close();
            */
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}