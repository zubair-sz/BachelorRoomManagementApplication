package com.brm.app.entity;

public enum PaymentStatus {
    /** Logged by the payer, waiting for the recipient/admin to confirm the money arrived. */
    PENDING,
    CONFIRMED,
    REJECTED
}
