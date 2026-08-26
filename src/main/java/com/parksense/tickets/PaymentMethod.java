package com.parksense.tickets;

/** How a fare was settled at the kiosk or booth. */
public enum PaymentMethod {
    CASH("Cash"),
    CARD("Card"),
    MOBILE("Mobile banking");

    private final String label;

    PaymentMethod(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
