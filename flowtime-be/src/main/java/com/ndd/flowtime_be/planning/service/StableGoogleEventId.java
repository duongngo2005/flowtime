package com.ndd.flowtime_be.planning.service;

import java.util.UUID;

final class StableGoogleEventId {

    private static final String PREFIX = "ft";

    private StableGoogleEventId() {}

    static String generate() {
        return PREFIX + UUID.randomUUID().toString().replace("-", "");
    }
}
