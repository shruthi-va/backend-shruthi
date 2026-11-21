package com.chubb.claims.claim.application.port.out;

import java.util.UUID;

public interface ProcessedEventRepository {

    boolean exists(UUID eventId);

    void markProcessed(UUID eventId);
}
