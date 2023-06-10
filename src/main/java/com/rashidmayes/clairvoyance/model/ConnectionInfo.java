package com.rashidmayes.clairvoyance.model;

public record ConnectionInfo(
        String host,
        int port,
        String username,
        String password,
        boolean useServicesAlternate
) {

    @Override
    public String toString() {
        return host + ":" + port;
    }

}
