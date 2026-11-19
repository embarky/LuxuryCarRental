package ch.unil.softarch.luxurycarrental.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;

/** Sends queued emails after their transaction has committed successfully. */
@ApplicationScoped
public class MailDispatcher {

    public void onCommitted(@Observes(during = TransactionPhase.AFTER_SUCCESS) MailEvent event) {
        if (event.pdf() != null && event.pdf().length > 0) {
            EmailSender.sendWithPdf(event.to(), event.email(), event.pdf(), event.pdfName());
        } else {
            EmailSender.send(event.to(), event.email());
        }
    }
}
