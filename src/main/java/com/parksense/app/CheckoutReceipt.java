package com.parksense.app;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * What the checkout page needs after a payment settles: the fee breakdown,
 * what was paid, the change, and how long the exit grace lasts.
 */
public record CheckoutReceipt(
        String ticketNo,
        String plate,
        String state,
        List<String[]> feeLines,
        BigDecimal total,
        String method,
        BigDecimal tendered,
        BigDecimal change,
        Instant paidAt,
        Instant graceUntil) {
}
