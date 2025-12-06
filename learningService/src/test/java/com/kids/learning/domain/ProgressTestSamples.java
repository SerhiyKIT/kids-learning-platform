package com.kids.learning.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class ProgressTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + (2 * Short.MAX_VALUE));

    public static Progress getProgressSample1() {
        return new Progress().id(1L).studentId(1L).score(1).status("status1");
    }

    public static Progress getProgressSample2() {
        return new Progress().id(2L).studentId(2L).score(2).status("status2");
    }

    public static Progress getProgressRandomSampleGenerator() {
        return new Progress()
            .id(longCount.incrementAndGet())
            .studentId(longCount.incrementAndGet())
            .score(intCount.incrementAndGet())
            .status(UUID.randomUUID().toString());
    }
}
