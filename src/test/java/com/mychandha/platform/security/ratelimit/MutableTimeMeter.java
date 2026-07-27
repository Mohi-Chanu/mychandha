package com.mychandha.platform.security.ratelimit;

import io.github.bucket4j.TimeMeter;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

final class MutableTimeMeter implements TimeMeter {

    private final AtomicLong nanoseconds = new AtomicLong();

    @Override
    public long currentTimeNanos() {
        return nanoseconds.get();
    }

    @Override
    public boolean isWallClockBased() {
        return true;
    }

    void advance(Duration duration) {
        nanoseconds.addAndGet(duration.toNanos());
    }
}
