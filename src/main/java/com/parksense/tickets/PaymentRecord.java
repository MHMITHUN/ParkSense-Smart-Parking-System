package com.parksense.tickets;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * One settled payment against a ticket. Cash payments carry the tendered
 * amount and change so the booth reconciliation adds up at day-close.
 */
public record PaymentRecord(String ticketNo, PaymentMethod method,
                            BigDecimal amount, BigDecimal tendered, BigDecimal changeDue,
                            Instant at) {
}
