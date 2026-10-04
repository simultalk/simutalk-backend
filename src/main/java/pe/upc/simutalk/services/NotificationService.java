package pe.upc.simutalk.services;

import pe.upc.simutalk.dtos.CandidateNotification;

/**
 * Port to the e-mail provider. Adapters live in infrastructure/external/mail. Sending is best effort: a
 * failure is logged and never interrupts the operation that triggered the notification.
 */
public interface NotificationService {

    void send(CandidateNotification notification);
}
