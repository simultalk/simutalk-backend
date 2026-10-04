package pe.upc.simutalk.serviceimpl;

import pe.upc.simutalk.dtos.CandidateNotification;

/** Subject and plain-text body of each candidate e-mail. */
final class NotificationTemplates {

    record Message(String subject, String text) {
    }

    private NotificationTemplates() {
    }

    static Message render(CandidateNotification notification) {
        var greeting = notification.recipientName().isEmpty() ? "Hola:" : "Hola, %s:".formatted(notification.recipientName());
        var title = notification.jobPostingTitle();
        return switch (notification.type()) {
            case INTERVIEW_INVITATION -> new Message(
                    "Te invitamos a la entrevista para «%s»".formatted(title),
                    """
                    %s

                    Tu postulación a «%s» avanzó a la etapa de entrevista. Ingresa a SimuTalk para responder la \
                    entrevista asincrónica cuando te acomode, antes de la fecha límite que verás en la plataforma.

                    Equipo de selección""".formatted(greeting, title));
            case SHORTLISTED -> new Message(
                    "Avanzaste a la terna final de «%s»".formatted(title),
                    """
                    %s

                    ¡Buenas noticias! Formas parte de la terna final para «%s». La empresa se pondrá en contacto \
                    contigo para los siguientes pasos.

                    Equipo de selección""".formatted(greeting, title));
            case REJECTED -> new Message(
                    "Actualización de tu postulación a «%s»".formatted(title),
                    """
                    %s

                    Gracias por tu interés en «%s». En esta oportunidad la empresa decidió continuar con otros \
                    perfiles. Puedes revisar la retroalimentación de tu entrevista en SimuTalk, si la hubo.

                    Equipo de selección""".formatted(greeting, title));
        };
    }
}
