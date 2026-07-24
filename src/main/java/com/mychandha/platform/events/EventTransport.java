package com.mychandha.platform.events;

public interface EventTransport {

    void publish(OutboxEnvelope event);
}
