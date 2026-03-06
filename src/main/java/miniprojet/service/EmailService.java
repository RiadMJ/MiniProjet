package miniprojet.service;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;

import lombok.extern.slf4j.Slf4j;

/**
 * Service d'envoi d'emails via l'API SendGrid.
 */
@Slf4j
@Service
public class EmailService {

    @Value("${sendgrid.api-key:NOT_SET}")
    private String sendGridApiKey;

    @Value("${sendgrid.from-email:noreply@miniprojet.sn}")
    private String fromEmail;

    @Value("${sendgrid.from-name:MiniProjet Pharmacie}")
    private String fromName;

    /**
     * Envoie un email via SendGrid
     *
     * @param to l'adresse email du destinataire
     * @param subject le sujet de l'email
     * @param body le contenu de l'email (HTML)
     * @return true si l'email a été envoyé avec succès
     */
    public boolean sendEmail(String to, String subject, String body) {
        if ("NOT_SET".equals(sendGridApiKey)) {
            log.warn("SendGrid API key non configurée. Email non envoyé à {}. Sujet: {}", to, subject);
            log.info("Contenu de l'email: {}", body);
            return false;
        }

        Email from = new Email(fromEmail, fromName);
        Email toEmail = new Email(to);
        Content content = new Content("text/html", body);
        Mail mail = new Mail(from, subject, toEmail, content);

        SendGrid sg = new SendGrid(sendGridApiKey);
        Request request = new Request();

        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sg.api(request);

            int statusCode = response.getStatusCode();
            if (statusCode >= 200 && statusCode < 300) {
                log.info("Email envoyé avec succès à {}. Status: {}", to, statusCode);
                return true;
            } else {
                log.error("Erreur lors de l'envoi de l'email à {}. Status: {}, Body: {}",
                        to, statusCode, response.getBody());
                return false;
            }
        } catch (IOException e) {
            log.error("Erreur IOException lors de l'envoi de l'email à {}: {}", to, e.getMessage());
            return false;
        }
    }
}
