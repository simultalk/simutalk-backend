package pe.upc.simutalk.serviceimpl;

import pe.upc.simutalk.dtos.CandidateNotification;
import pe.upc.simutalk.enums.NotificationType;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificationServiceImplTest {

    private static final CandidateNotification SHORTLISTED = new CandidateNotification(NotificationType.SHORTLISTED,
            "rosa@example.com", "Rosa", "Analista de Datos");

    private static NotificationServiceImpl service(String mode, String baseUrl, String apiKey, String sender) {
        return new NotificationServiceImpl(mode, baseUrl, apiKey, sender, "SimuTalk", Duration.ofMillis(500), RestClient.builder());
    }

    @Test
    void mockModeOnlyLogsAndNeverTouchesTheNetwork() throws Exception {
        var calls = new AtomicInteger();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            calls.incrementAndGet();
            exchange.sendResponseHeaders(201, -1);
            exchange.close();
        });
        server.start();
        try {
            service("mock", "http://127.0.0.1:" + server.getAddress().getPort(), "", "").send(SHORTLISTED);
            assertThat(calls).hasValue(0);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void liveModePostsTheMailToBrevo() throws Exception {
        var path = new AtomicReference<String>();
        var apiKey = new AtomicReference<String>();
        var body = new AtomicReference<String>();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            path.set(exchange.getRequestMethod() + " " + exchange.getRequestURI());
            apiKey.set(exchange.getRequestHeaders().getFirst("api-key"));
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            var response = "{\"messageId\":\"<1@smtp-relay>\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(201, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            service("live", "http://127.0.0.1:" + server.getAddress().getPort(), "brevo-key", "seleccion@simutalk.example")
                    .send(SHORTLISTED);

            assertThat(path.get()).isEqualTo("POST /v3/smtp/email");
            assertThat(apiKey.get()).isEqualTo("brevo-key");
            assertThat(body.get()).contains("\"sender\":{", "seleccion@simutalk.example", "\"to\":[{",
                    "rosa@example.com", "Avanzaste a la terna final de «Analista de Datos»", "\"textContent\"");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void aProviderFailureIsLoggedAndNeverThrown() throws Exception {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            exchange.sendResponseHeaders(500, -1);
            exchange.close();
        });
        server.start();
        try {
            var live = service("live", "http://127.0.0.1:" + server.getAddress().getPort(), "k", "s@simutalk.example");
            assertThatCode(() -> live.send(SHORTLISTED)).doesNotThrowAnyException();
            var unreachable = service("live", "http://127.0.0.1:1", "k", "s@simutalk.example");
            assertThatCode(() -> unreachable.send(SHORTLISTED)).doesNotThrowAnyException();
        } finally {
            server.stop(0);
        }
    }

    @Test
    void liveModeRequiresTheApiKeyAndTheSender() {
        assertThatThrownBy(() -> service("live", "http://127.0.0.1:1", "", "s@simutalk.example"))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> service("live", "http://127.0.0.1:1", "k", " ")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> service("smtp", "http://127.0.0.1:1", "k", "s")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void logsNeverShowTheFullAddress() {
        assertThat(NotificationServiceImpl.mask("rosa.quispe@example.com")).isEqualTo("r***@example.com");
        assertThat(NotificationServiceImpl.mask("nope")).isEqualTo("***");
    }

    @Test
    void eachNotificationHasItsOwnMessage() {
        assertThat(NotificationTemplates.render(new CandidateNotification(NotificationType.INTERVIEW_INVITATION,
                "r@example.com", "Rosa", "Analista")).subject()).isEqualTo("Te invitamos a la entrevista para «Analista»");
        assertThat(NotificationTemplates.render(new CandidateNotification(NotificationType.REJECTED,
                "r@example.com", "Rosa", "Analista")).text()).startsWith("Hola, Rosa:").contains("otros perfiles");
    }
}
