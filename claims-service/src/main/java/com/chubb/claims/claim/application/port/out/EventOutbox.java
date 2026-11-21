package com.chubb.claims.claim.application.port.out;

import com.chubb.claims.common.event.DomainEvent;

public interface EventOutbox {

    void append(String topic, String key, DomainEvent<?> event);
}
