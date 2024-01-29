package de.tukl.softech.exclaim.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;


@Component
public class Mail {

    private final JavaMailSender emailSender;

    @Value("${exclaim.mail.sender}")
    private String sender;

    @Value("${exclaim.linkprefix}")
    private String linkPrefix;

    public Mail(JavaMailSender emailSender) {
        this.emailSender = emailSender;
    }

    private void send(String recipient, String subject, String text, boolean htmlContent) throws MailException {
        MimeMessage mimeMessage = emailSender.createMimeMessage();
        MimeMessageHelper msg = new MimeMessageHelper(mimeMessage, "utf-8");
        try {
            msg.setTo(recipient);
            msg.setSubject(subject);
            msg.setText(text, htmlContent);
            msg.setFrom(sender);
        } catch (MessagingException e) {
            throw new MailPreparationException("Error sending email", e);
        }
        emailSender.send(mimeMessage);
    }


    public void sendActivationLink(String recipient, String username, String lastName, String firstName, String code) throws MailException {

        String url = createVerificationLink(username, code);

        String subject = "[Exclaim] Aktivierung des Benutzerkontos '" + username + "'";

        String text =
                "<p>Diese Email wurde vom Exclaim System der AG Softwaretechnik der TU Kaiserslautern verschickt.\n" +
                "Für diese Emailadresse soll der Benutzer '" + username + "' (" + firstName + " " + lastName + ") angelegt werden.</p>\n\n" +
                "<p>Wenn Sie diese Email nicht durch eine Registrierung bei unserem System angefordet haben, können Sie sie einfach ignorieren und löschen.</p>\n\n" +
                "<p>Zur Aktivierung des Kontos klicken Sie bitte auf den folgenden Link:</p>\n" +
                        "<p><a href=\"" + url + "\">" + url + "</a></p>\n\n" +
                "<p>This email was send for the purpose of email validation, for an account registered at the Exclaim system of the Software Technology Group at the University of Kaiserslautern. If you did not issue this registration, you can ignore and delete this email.</p>";

    	send(recipient, subject, text, true);

    }

    public String createVerificationLink(String username, String code) {
        UriComponentsBuilder builder = ServletUriComponentsBuilder.fromUriString(linkPrefix);
        builder.path("/activate");
        builder.queryParam("user", username);
        builder.queryParam("code", code);
        return builder.build().toString();
    }

    public void sendResetLink(String recipient, String username, String reset) throws MailException {

        UriComponentsBuilder builder = ServletUriComponentsBuilder.fromUriString(linkPrefix);
        builder.path("/resetPassword");
        builder.queryParam("user", username);
        builder.queryParam("reset", reset);
        String url = builder.build().toString();

    	String subject = "[Exclaim] Zurücksetzen des Passworts";

        String text =
                "<p>Diese Email wurde vom Exclaim System der AG Softwaretechnik der TU Kaiserslautern verschickt.\n" +
                "Das Passwort für das mit dieser Emailadresse verknüpfte Exclaim-Konto (Benutzername: " + username+ ") soll zurückgesetzt werden.</p>\n\n" +
                "<p>Wenn Sie die Zurücksetzung des Passworts nicht angefordert haben, können Sie diese Email einfach ignorieren und löschen.</p>\n\n" +
                "<p>Um das Passwort für Ihr Exclaim-Konto zurückzusetzen und neue Login-Daten zu erhalten, klicken Sie bitte auf den folgenden Link:</p>\n" +
                        "<p><a href=\"" + url + "\">" + url + "</a></p>\n\n";

        send(recipient, subject, text, true);
    }

    public void sendNewLoginData(String recipient, String username, String passwordClear) throws MailException {

    	String subject = "[Exclaim] Login Daten";

        String text =
                "Diese Email wurde vom Exclaim System der AG Softwaretechnik der TU Kaiserslautern verschickt.\n" +
                "Das Passwort für das mit dieser Emailadresse verknüpfte Exclaim-Konto wurde zurückgesetzt.\n\n" +
                "Die Login-Daten lauten:\n\n" +
                "Benutzername:\t"+username+"\n\n" +
                "Passwort:\t"+passwordClear+"\n\n\n" +
                "Bitte ändern Sie das Passwort nach Ihrem nächsten Login.\n\n";

        send(recipient, subject, text, false);
    	
    }   
    
}