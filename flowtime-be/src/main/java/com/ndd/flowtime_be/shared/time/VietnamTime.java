package com.ndd.flowtime_be.shared.time;

import java.time.ZoneId;

/** FlowTime currently schedules exclusively in Vietnam time. */
public final class VietnamTime {

    public static final String ZONE_ID = "Asia/Ho_Chi_Minh";
    public static final ZoneId ZONE = ZoneId.of(ZONE_ID);

    private VietnamTime() {
    }
}
