package com.kids.platform.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class ParentTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    public static Parent getParentSample1() {
        return new Parent().id(1L).firstName("firstName1").email("email1");
    }

    public static Parent getParentSample2() {
        return new Parent().id(2L).firstName("firstName2").email("email2");
    }

    public static Parent getParentRandomSampleGenerator() {
        return new Parent().id(longCount.incrementAndGet()).firstName(UUID.randomUUID().toString()).email(UUID.randomUUID().toString());
    }
}
