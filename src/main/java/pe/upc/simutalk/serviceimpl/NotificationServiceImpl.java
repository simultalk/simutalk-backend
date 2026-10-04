package pe.upc.simutalk.serviceimpl;

import pe.upc.simutalk.dtos.CandidateNotification;

import pe.upc.simutalk.services.NotificationService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Adapter of the {@link NotificationService} port for Brevo's transactional e-mail API.
 * <ul>
 *   <li>{@code mail.mode=mock} (default): no network; it only logs what it would have sent (type, masked
 *       recipient and subject; the body at DEBUG).</li>
 *   <li>{@code mail.mode=live}: {@code POST {mail.brevo.base-url}/v3/smtp/email} with the {@code api-key}
 *       header; requires {@code BREVO_API_KEY} and {@code MAIL_SENDER_EMAIL}.</li>
 * </ul>
 * Never throws: a failed delivery is logged as WARN, so it never interrupts the operation that triggered it.
 */
@Slf4j
@Service
public class NotificationServiceImpl implements NotificationService {

    enum Mode { MOCK, LIVE }

    private final Mode mode;
    private final String apiKey;
    private final String senderEmail;
    private final String senderName;
    private final RestClient restClient;

    public NotificationServiceImpl(@Value("${mail.mode:mock}") String mode,
                                   @Value("${mail.brevo.base-url:https://api.brevo.com}") String baseUrl,
                                   @Value("${mail.brevo.api-key:}") String apiKey,
                                   @Value("${mail.sender.email:}") String senderEmail,
                                   @Value("${mail.sender.name:SimuTalk}") String senderName,
                                   @Value("${mail.timeout:5s}") Duration timeout,
                                   RestClient.Builder restClientBuilder) {
        this.mode = parseMode(mode);
        this.apiKey = apiKey;
        this.senderEmail = senderEmail;
        this.senderName = senderName;
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);
        this.restClient = restClientBuilder.baseUrl(baseUrl).requestFactory(requestFactory).build();
        if (this.mode == Mode.LIVE && (isBlank(apiKey) || isBlank(senderEmail))) {
            throw new IllegalStateException("mail.mode=live requires BREVO_API_KEY and MAIL_SENDER_EMAIL");
        }
        log.info("Candidate notifications running in {} mode", this.mode);
    }

    @Override
    public void send(CandidateNotification notification) {
        var message = NotificationTemplates.render(notification);
        if (mode == Mode.MOCK) {
            log.info("Mail (mock, not sent) {} to {}: \"{}\"", notification.type(), mask(notification.recipientEmail()),
                    message.subject());
            log.debug("Mail (mock) body:\n{}", message.text());
            return;
        }
        try {
            restClient.post()
                    .uri("/v3/smtp/email")
                    .header("api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "sender", Map.of("name", senderName, "email", senderEmail),
                            "to", List.of(Map.of("email", notification.recipientEmail(), "name", notification.recipientName())),
                            "subject", message.subject(),
                            "textContent", message.text()))
                    .retrieve()
                    .toBodilessEntity();
            log.info("Mail {} sent to {}", notification.type(), mask(notification.recipientEmail()));
        } catch (RuntimeException ex) {
            log.warn("Mail {} to {} could not be sent: {}", notification.type(), mask(notification.recipientEmail()),
                    ex.toString());
        }
    }

    /** Keeps logs free of full addresses: {@code rosa@example.com} becomes {@code r***@example.com}. */
    static String mask(String email) {
        var at = email.indexOf('@');
        return at <= 0 ? "***" : email.charAt(0) + "***" + email.substring(at);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static Mode parseMode(String mode) {
        try {
            return Mode.valueOf(mode.strip().toUpperCase(Locale.ROOT));
        } catch (RuntimeException ex) {
            throw new IllegalStateException("mail.mode must be 'mock' or 'live', got: " + mode);
        }
    }
}
